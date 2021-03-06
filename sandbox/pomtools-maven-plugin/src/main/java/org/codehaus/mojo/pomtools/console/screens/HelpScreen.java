package org.codehaus.mojo.pomtools.console.screens;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Iterator;

import org.codehaus.mojo.pomtools.console.toolkit.ConsoleEvent;
import org.codehaus.mojo.pomtools.console.toolkit.ConsoleExecutionException;
import org.codehaus.mojo.pomtools.console.toolkit.ConsoleScreen;
import org.codehaus.mojo.pomtools.console.toolkit.ConsoleScreenDisplay;
import org.codehaus.mojo.pomtools.console.toolkit.ConsoleUtils;
import org.codehaus.mojo.pomtools.console.toolkit.event.CatchAllListener;
import org.codehaus.mojo.pomtools.console.toolkit.event.ConsoleEventDispatcher;
import org.codehaus.mojo.pomtools.console.toolkit.event.ConsoleEventListener;
import org.codehaus.mojo.pomtools.console.toolkit.widgets.TableColumn;
import org.codehaus.mojo.pomtools.console.toolkit.widgets.TableLayout;

/**
 * 
 * @author <a href="mailto:dhawkins@codehaus.org">David Hawkins</a>
 * @version $Id$
 */
public class HelpScreen
    extends AbstractModelScreen
{
    public static final int HELP_TEXT_MAX_WIDTH = 80;
    
    private ConsoleScreen helpForScreen;
    
    public HelpScreen( ConsoleScreen helpForScreen )
    {
        super( null );
        this.helpForScreen = helpForScreen;
    }

    public ConsoleScreenDisplay getDisplay()
        throws ConsoleExecutionException
    {
        final int rightColumnWidth = 60;
        
        StringBuffer sb = new StringBuffer();

        sb.append( getHeader( "Help for " + helpForScreen.getName() ) );
        
        String helpText = helpForScreen.getHelpText();
        if ( helpText != null )
        {
            sb.append( ConsoleUtils.wordWrap( helpText, HELP_TEXT_MAX_WIDTH ) )
              .append( NEWLINE )
              .append( NEWLINE );
        }

        // Table of Keys
        TableLayout tab = new TableLayout( getTerminal(), new TableColumn[] {
                TableColumn.ALIGN_LEFT_COLUMN,
                new TableColumn( rightColumnWidth ) } );
        
        tab.add( new String[] { "Commands", "Description" } );

        for ( Iterator i = helpForScreen.getEventDispatcher().getListeners().iterator(); i.hasNext(); )
        {
            ConsoleEventListener c = (ConsoleEventListener) i.next();
            tab.add( new String[] { c.getDescriptionKey(), c.getDescription() } );
        }

        sb.append( tab.getOutput() );

        return createDisplay( sb.toString(), PRESS_ENTER_TO_CONTINUE, false );
    }

    public ConsoleEventDispatcher getEventDispatcher()
        throws ConsoleExecutionException
    {
        return new ConsoleEventDispatcher().add( new CatchAllListener()
        {
            public void processEvent( ConsoleEvent event )
                throws ConsoleExecutionException
            {
                event.setReturnToPreviousScreen();
            }
        } );
    }

}
