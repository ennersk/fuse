/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.fab.osgi.bnd;

import aQute.lib.osgi.Analyzer;
import aQute.lib.spring.XMLType;
import aQute.lib.spring.XMLTypeProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Bnd plugin to add imports for imported resources from CXF
 */
public class CXFNamespaceHandlerPlugin extends XMLTypeProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CXFNamespaceHandlerPlugin.class);

    @Override
    protected List<XMLType> getTypes(Analyzer analyzer) throws Exception {
        List<XMLType> types = new ArrayList<XMLType>();

        try {
            String header = analyzer.getProperty("Spring-Context", "META-INF/spring");
            process(types,"cxf.xsl", header, ".*\\.xml");
        } catch (Exception e) {
            LOGGER.warn("Error while adding bundle imports for CXF namespace elements", e);
        }

        return types;
    }

}
