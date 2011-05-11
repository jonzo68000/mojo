package org.apache.maven.diagrams.gui.renderers;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/**
 * @author <a href="mailto:ptab@newitech.com">Piotr Tabor</a>
 * @version $Id$
 */
public class RendererNodeCacheImpl implements RendererNodeCache
{
    private Double height;

    private Double width;

    public RendererNodeCacheImpl()
    {
        height = null;
        width = null;
    }

    public Double getNodeHeight()
    {
        return height;
    }

    public Double getNodeWidth()
    {

        return width;
    }

    public void setNodeHeight( Double a_height )
    {
        height = a_height;
    }

    public void setNodeWidth( Double a_width )
    {
        width = a_width;
    }

}
