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
/*
 * (c) Copyright 2009 Talis Information Ltd.
 * (c) Copyright 2010 Epimorphics Ltd.
 * All rights reserved.
 * [See end of file]
 */
package org.apache.rya.jena.jenasesame;

import org.apache.rya.jena.jenasesame.impl.GraphRepository;
import org.apache.rya.jena.jenasesame.impl.JenaSesameDatasetGraph;
import org.apache.rya.jena.jenasesame.impl.JenaSesameQueryEngineFactory;
import org.openrdf.model.Resource;
import org.openrdf.repository.RepositoryConnection;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.engine.QueryEngineFactory;
import com.hp.hpl.jena.sparql.engine.QueryEngineRegistry;

/**
 * Jena API over Sesame repository
 */
public class JenaSesame {
    private static boolean isInitialized = false;
    private static QueryEngineFactory factory = new JenaSesameQueryEngineFactory();
    static {
        init ();
    }

    private static void init() {
        if (!isInitialized) {
            isInitialized = true;
            QueryEngineRegistry.addFactory(factory);
        }
    }

    /**
     * Create a Model that is backed by a repository.
     * The model is the triples seen with no specification of the context.
     * @param connection the {@link RepositoryConnection}.
     * @return the {@link Model}.
     */
    public static Model createModel(final RepositoryConnection connection) {
        final Graph graph = new GraphRepository(connection);
        return ModelFactory.createModelForGraph(graph);
    }

    /**
     * Create a model that is backed by a repository.
     * The model is the triples seen with specified context.
     * @param connection the {@link RepositoryConnection}.
     * @param context the {@link Resource} context.
     * @return the {@link Model}.
     */
    public static Model createModel(final RepositoryConnection connection, final Resource context) {
        final Graph graph =  new GraphRepository(connection, context);
        return ModelFactory.createModelForGraph(graph);
    }

    /**
     * Create a dataset that is backed by a repository
     * @param connection the {@link RepositoryConnection}.
     * @return the {@link Dataset}.
     */
    public static Dataset createDataset(final RepositoryConnection connection) {
        final DatasetGraph dsg = new JenaSesameDatasetGraph(connection);
        return DatasetFactory.create(dsg);
    }
}

/*
 * (c) Copyright 2009 Talis Information Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */