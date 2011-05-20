package org.codehaus.mojo.jsimport;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFileFilterRequest;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.codehaus.plexus.util.Scanner;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Mojo for generating properties for filtering into html script elements. This uses the serialised file dependency
 * graph provided by the import mojo.
 */
public abstract class AbstractGenerateHtmlMojo
    extends AbstractMojo
{
    /**
     * The current project scope.
     */
    protected enum Scope
    {
        /** */
        COMPILE,
        /** */
        TEST
    };

    /**
     * The local repo.
     * 
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * HTML file extensions.
     * 
     * @parameter default-value="**\/*.html,**\/*.htm"
     * @required
     */
    private String htmlResourceExtensions;

    /**
     * The target path for js files.
     * 
     * @parameter default-value="js"
     * @required
     */
    private String targetJsPath;

    /**
     * The character encoding scheme to be applied when filtering resources.
     * 
     * @parameter expression="${encoding}" default-value="${project.build.sourceEncoding}"
     * @required
     */
    private String encoding;

    /**
     * The build context so that we can tell Maven certain files have changed if required.
     * 
     * @component
     */
    private BuildContext buildContext;

    /**
     * The Maven filtering to use.
     * 
     * @component
     */
    private MavenFileFilter mavenFileFilter;

    /**
     * A graph of filenames and their dependencies.
     */
    private final Map<String, LinkedHashSet<String>> fileDependencies = new HashMap<String, LinkedHashSet<String>>();

    /**
     * As a above but for compile time dependencies only.
     */
    private final Map<String, LinkedHashSet<String>> compileFileDependencies =
        new HashMap<String, LinkedHashSet<String>>();

    /**
     * Given a set of file paths, build a new set of any dependencies each of these paths may have, and any dependencies
     * that these dependencies have etc.
     * 
     * @param a set of nodes already visited so as to avoid overflow.
     * @param filePaths the set of file paths to iterate over.
     * @param allImports the set to build.
     */
    private void buildImportsRecursively( Set<String> visitedNodes, LinkedHashSet<String> filePaths,
                                          LinkedHashSet<String> allImports )
    {
        for ( String filePath : filePaths )
        {
            if ( !visitedNodes.contains( filePath ) )
            {
                visitedNodes.add( filePath );

                LinkedHashSet<String> filePathDependencies = fileDependencies.get( filePath );
                if ( filePathDependencies == null && compileFileDependencies != null )
                {
                    filePathDependencies = compileFileDependencies.get( filePath );
                }
                if ( filePathDependencies != null )
                {
                    buildImportsRecursively( visitedNodes, filePathDependencies, allImports );
                }
                allImports.add( filePath );
            }
        }
    }

    /**
     * Copy files over from the local repo to the target folders. We only do the copy if the modification date of the
     * source file is greater than the destination file.
     * 
     * @param localRepoFilesToCopy the files to copy. This is a mapping of source file to target file.
     * @throws MojoExecutionException if there is an IO issue.
     */
    private void copyLocalRepoFilesToTarget( Map<String, String> localRepoFilesToCopy, File targetFolder )
        throws MojoExecutionException
    {
        for ( Map.Entry<String, String> entry : localRepoFilesToCopy.entrySet() )
        {
            try
            {
                File sourceFile = new File( entry.getKey() );
                File targetFile = new File( targetFolder, entry.getValue() );
                if ( sourceFile.lastModified() > targetFile.lastModified() )
                {
                    FileUtils.copyFile( sourceFile, targetFile );
                    if ( getLog().isDebugEnabled() )
                    {
                        getLog().debug( "Copying file: " + sourceFile.getAbsolutePath() );
                    }
                }
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "While copying files: ", e );
            }
        }
    }

    /**
     * Perform the goal of this mojo.
     * 
     * @param sourceJsFolder the folder where the source js files reside.
     * @param mainSourceJsFolder the folder where the main source js files reside.
     * @param htmlResourceFolder the folder where the resource html files reside.
     * @param targetFolder where all files are going to end up.
     * @param workFolder the folder where our work files can be found.
     * @param scope scope the scope of the dependencies we are to search for.
     * @throws MojoExecutionException if there is an execution failure.
     */
    public void doExecute( File sourceJsFolder, File mainSourceJsFolder, File htmlResourceFolder, //
                           File targetFolder, File workFolder, Scope scope )
        throws MojoExecutionException
    {
        // Load the dependency graph. Note fileAssignedGlobals is unused by this
        // mojo but a the utility below is contracted to supply it.
        Map<String, String> fileAssignedGlobals = new HashMap<String, String>();
        long fileDependencyGraphModificationTime =
            FileDependencyPersistanceUtil.readFileDependencyGraph( workFolder, fileDependencies, fileAssignedGlobals );

        // If we are in test scope then also load in the compile scoped dependency information as we need to resolve
        // against this also.
        if ( scope == Scope.TEST )
        {
            Map<String, String> compileFileAssignedGlobals = new HashMap<String, String>();
            FileDependencyPersistanceUtil.readFileDependencyGraph( new File( workFolder.getParentFile(), "main" ),
                                                                   compileFileDependencies, //
                                                                   compileFileAssignedGlobals );
        }

        // Generate properties relating to the file dependency graph and form a map between source and target files that
        // should be copied from the local repo.
        Map<String, String> localRepoFilesToCopy = new HashMap<String, String>();
        Properties fileDependencyProperties =
            generateProperties( sourceJsFolder, mainSourceJsFolder, localRepoFilesToCopy );

        // Copy local repo files to the target folder
        copyLocalRepoFilesToTarget( localRepoFilesToCopy, targetFolder );

        // Filter all of our HTML file's script statements and generate them into the target folder.
        generateHtmlWithProperties( fileDependencyProperties, fileDependencyGraphModificationTime, htmlResourceFolder,
                                    targetFolder );

    }

    // The last step is to refresh all of the html files in the resource folder if our dependencies are more recent.
    // This is because we need to ensure that html files have their script elements re-generated correctly.
    private void generateHtmlWithProperties( Properties fileDependencyProperties,
                                             long fileDependencyGraphModificationTime, File htmlResourceFolder,
                                             File targetFolder )
    {
        Scanner scanner = buildContext.newScanner( htmlResourceFolder, true );
        scanner.setIncludes( htmlResourceExtensions.split( "," ) );
        scanner.scan();
        List<String> includedFiles = Arrays.asList( scanner.getIncludedFiles() );
        boolean htmlFilesFiltered = false;
        int lastNestedFolderCount = -1;

        MavenFileFilterRequest filterRequest = new MavenFileFilterRequest();
        filterRequest.setFiltering( true );

        for ( String resourceFile : includedFiles )
        {
            File destinationFile = new File( targetFolder, resourceFile );
            File sourceFile = new File( htmlResourceFolder, resourceFile );
            if ( htmlFilesFiltered || sourceFile.lastModified() > destinationFile.lastModified()
                || destinationFile.lastModified() < fileDependencyGraphModificationTime )
            {
                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( "Applying import filter to: " + resourceFile );
                }

                // Conditionally apply a map of properties that has the correct relative path expression to each js
                // file.
                int nestedFolderCount = StringUtils.countMatches( resourceFile, "/" );
                if ( nestedFolderCount != lastNestedFolderCount )
                {
                    StringBuilder sb = new StringBuilder();
                    for ( int i = 0; i < nestedFolderCount; ++i )
                    {
                        sb.append( "../" );
                    }
                    String relativePath = sb.toString();
                    Properties relativeFileDependencyProperties = new Properties();
                    for ( Map.Entry<Object, Object> keyValue : fileDependencyProperties.entrySet() )
                    {
                        relativeFileDependencyProperties.put( keyValue.getKey(),
                                                              relativePath + ( (String) keyValue.getValue() )//
                                                              .replaceAll( "src=\"", "src=\"" + relativePath ) );
                    }
                    filterRequest.setAdditionalProperties( relativeFileDependencyProperties );

                    lastNestedFolderCount = nestedFolderCount;
                }

                // Copy the file with filtering.
                filterRequest.setFrom( sourceFile );

                destinationFile.getParentFile().mkdirs();
                filterRequest.setTo( destinationFile );

                try
                {
                    mavenFileFilter.copyFile( filterRequest );
                    buildContext.refresh( destinationFile );
                    htmlFilesFiltered = true;
                }
                catch ( MavenFilteringException e )
                {
                    getLog().error( e );
                }
            }
        }
        if ( htmlFilesFiltered )
        {
            getLog().info( "Dependencies changed. " + includedFiles.size() + " file(s) re-filtered." );
        }
    }

    /**
     * Generate the file dependency properties and also populate a mapping of repository files to be copied into the
     * target folder.
     * 
     * @param sourceJsFolder the folder where the source js files reside.
     * @param mainSourceJsFolder the folder where the main source js files reside.
     * @param scope scope the scope of the dependencies we are to search for.
     * @param localRepoFilesToCopy the mapping of source files that should be copied to the target folder
     * @throws MojoExecutionException if there is an execution failure.
     */
    private Properties generateProperties( File sourceJsFolder, File mainSourceJsFolder,
                                           Map<String, String> localRepoFilesToCopy )
        throws MojoExecutionException
    {
        Properties fileDependencyProperties = new Properties();

        String sourceFolderPath = sourceJsFolder.getAbsolutePath();
        if ( sourceFolderPath.length() == 0 )
        {
            return fileDependencyProperties;
        }
        String mainSourceFolderPath = mainSourceJsFolder.getAbsolutePath();
        if ( mainSourceFolderPath.length() == 0 )
        {
            return fileDependencyProperties;
        }

        String localRepositoryPath = localRepository.getBasedir();

        for ( Map.Entry<String, LinkedHashSet<String>> entry : fileDependencies.entrySet() )
        {
            String jsFile = entry.getKey();

            // Make our source files available for script elements.
            if ( jsFile.startsWith( sourceFolderPath ) )
            {
                // Build a new set of imports for this current js file and all
                // of its imports and their imports etc.
                Set<String> visitedNodes = new HashSet<String>();
                LinkedHashSet<String> allImports = new LinkedHashSet<String>();
                buildImportsRecursively( visitedNodes, entry.getValue(), allImports );

                // Build a set of script statements for filtering into HTML
                // files.
                String closeOpenScriptDeclaration = "\"></script><script type=\"text/javascript\" src=\"";

                StringBuilder propertyValue = new StringBuilder();
                for ( String importFile : allImports )
                {
                    // Make the file path relative.
                    String relativeImportFile;
                    if ( importFile.startsWith( sourceFolderPath ) )
                    {
                        relativeImportFile = targetJsPath + importFile.substring( sourceFolderPath.length() );
                    }
                    else if ( importFile.startsWith( localRepositoryPath ) )
                    {
                        relativeImportFile = targetJsPath + importFile.substring( localRepositoryPath.length() );
                        // Flag this file for copying as long as the file belongs to our scope. If the compile time
                        // dependencies are null then we are in compile scope. Otherwise we are in test scope, in which
                        // case we only copy the file if the dependency belongs to the test scope.
                        if ( compileFileDependencies == null || //
                            ( compileFileDependencies != null && //
                            !compileFileDependencies.containsKey( importFile ) ) )
                        {
                            localRepoFilesToCopy.put( importFile, relativeImportFile );
                        }
                    }
                    else if ( importFile.startsWith( mainSourceFolderPath ) )
                    {
                        relativeImportFile = targetJsPath + importFile.substring( mainSourceFolderPath.length() );
                    }
                    else
                    {
                        throw new MojoExecutionException( "Unexpected import file path "
                            + "(not project relative or local repo): " + importFile );
                    }

                    if ( propertyValue.length() > 0 )
                    {
                        propertyValue.append( closeOpenScriptDeclaration );
                    }
                    propertyValue.append( relativeImportFile );
                }
                if ( propertyValue.length() > 0 )
                {
                    propertyValue.append( closeOpenScriptDeclaration );
                }

                // Properties are always site relative and expressed without a /js to make it simple for substitution.
                String propertyName;
                if ( sourceFolderPath.length() > 1 )
                {
                    propertyName = jsFile.substring( sourceFolderPath.length() + 1 );
                }
                else
                {
                    propertyName = jsFile;
                }

                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( "Generating script statements for files: " + allImports
                                        + " relating to filter property: " + propertyName );
                }

                propertyValue.append( targetJsPath + "/" + propertyName );
                fileDependencyProperties.setProperty( propertyName, propertyValue.toString() );
            }
        }

        return fileDependencyProperties;
    }

    /**
     * @return property.
     */
    public BuildContext getBuildContext()
    {
        return buildContext;
    }

    /**
     * @return property.
     */
    public String getEncoding()
    {
        return encoding;
    }

    /**
     * @return property.
     */
    public Map<String, LinkedHashSet<String>> getFileDependencies()
    {
        return fileDependencies;
    }

    /**
     * @return property.
     */
    public String getHtmlResourceExtensions()
    {
        return htmlResourceExtensions;
    }

    /**
     * @return property.
     */
    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    /**
     * @return property.
     */
    public MavenFileFilter getMavenFileFilter()
    {
        return mavenFileFilter;
    }

    /**
     * @return property.
     */
    public String getTargetJsPath()
    {
        return targetJsPath;
    }

    /**
     * @param buildContext set property.
     */
    public void setBuildContext( BuildContext buildContext )
    {
        this.buildContext = buildContext;
    }

    /**
     * @param encoding set property.
     */
    public void setEncoding( String encoding )
    {
        this.encoding = encoding;
    }

    /**
     * @param htmlResourceExtensions set property.
     */
    public void setHtmlResourceExtensions( String htmlResourceExtensions )
    {
        this.htmlResourceExtensions = htmlResourceExtensions;
    }

    /**
     * @param localRepository set property.
     */
    public void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    /**
     * @param mavenFileFilter set property.
     */
    public void setMavenFileFilter( MavenFileFilter mavenFileFilter )
    {
        this.mavenFileFilter = mavenFileFilter;
    }

    /**
     * @param targetJsPath set property.
     */
    public void setTargetJsPath( String targetJsPath )
    {
        this.targetJsPath = targetJsPath;
    }

}
