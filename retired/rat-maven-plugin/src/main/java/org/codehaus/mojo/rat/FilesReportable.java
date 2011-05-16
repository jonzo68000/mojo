package org.codehaus.mojo.rat;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import rat.document.IDocument;
import rat.document.IDocumentCollection;
import rat.document.impl.DocumentImplUtils;
import rat.document.impl.zip.ZipDocumentFactory;
import rat.report.IReportable;
import rat.report.RatReport;
import rat.report.RatReportFailedException;

/**
 * Implementation of IReportable that traverses over a set of files.
 */
class FilesReportable implements IReportable
{
    private final File basedir;

    private final String[] files;

    FilesReportable( File basedir, String[] files )
            throws IOException
    {
        final File currentDir = new File( System.getProperty( "user.dir" ) ).getCanonicalFile();
        final File f = basedir.getCanonicalFile();
        if ( currentDir.equals( f ) )
        {
            this.basedir = null;
        }
        else
        {
            this.basedir = basedir;
        }
        this.files = files;
    }

    public void run( RatReport report ) throws RatReportFailedException
    {
        FileDocument document = new FileDocument();
        for ( int i = 0; i < files.length; i++ )
        {
            document.setFile( new File( basedir, files[i] ) );
            report.report( document );
        }
    }

    private class FileDocument implements IDocument
    {
        private File file;

        void setFile( File file )
        {
            this.file = file;
        }

        public IDocumentCollection readArchive() throws IOException
        {
            return ZipDocumentFactory.load( file );
        }

        public Reader reader() throws IOException
        {
            final InputStream in = new FileInputStream( file );
            return new InputStreamReader( in );
        }

        public String getName()
        {
            return DocumentImplUtils.toName( file );
        }
    }
}