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
// Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// This source code is available under the terms of the Affero General Public
// License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of
// proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package org.apache.rya.jena.example.pellet;

import static org.apache.rya.jena.example.pellet.util.ExampleUtils.createRyaStatement;

import java.util.List;

import org.apache.rya.api.domain.RyaStatement;
import org.apache.rya.api.persist.RyaDAO;
import org.apache.rya.jena.example.pellet.util.ExampleUtils;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.openrdf.repository.sail.SailRepositoryConnection;

import com.clarkparsia.pellet.sparqldl.jena.SparqlDLExecutionFactory;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * <p>
 * Title: SPARQLDLExample
 * </p>
 * <p>
 * Description: This program shows how to use the Pellet SPARQL-DL engine
 * </p>
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 */
public class SPARQLDLExample implements PelletExampleRunner {
    // The ontology loaded as dataset
    private static final String FILE = "file:src/main/resources/data/university0-0.owl";

    private static final String[] QUERIES = new String[] {
            // One of the original LUBM queries
            "data/lubm-query4.sparql",
            // A SPARQL-DL query
            "data/lubm-sparql-dl.sparql",
            // A SPARQL-DL with the SPARQL-DL extensions vocabulary
            "data/lubm-sparql-dl-extvoc.sparql" };

    private final SailRepositoryConnection conn;

    /**
     * Creates a new instance of {@link SPARQLDLExample}.
     * @param conn the {@link SailRepositoryConnection}.
     */
    public SPARQLDLExample(final SailRepositoryConnection conn) {
        this.conn = conn;
    }

    @Override
    public void run() throws Exception {
        run(FILE, QUERIES);
    }

    public void run(final String file, final String[] queries) throws Exception {
        for (final String query : queries) {
            // First create a Jena ontology model backed by the Pellet reasoner
            // (note, the Pellet reasoner is required)
            final OntModel m = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );

            // Then read the data from the file into the ontology model
            m.read( file );

            // Now read the query file into a query object
            final Query q = QueryFactory.read( query );

            // Create a SPARQL-DL query execution for the given query and
            // ontology model
            final QueryExecution qe = SparqlDLExecutionFactory.create( q, m );

            // We want to execute a SELECT query, do it, and return the result set
            final ResultSet rs = qe.execSelect();

            // Print the query for better understanding
            System.out.println(q.toString());

            // There are different things we can do with the result set, for
            // instance iterate over it and process the query solutions or, what we
            // do here, just print out the results
            ResultSetFormatter.out( rs );

            // And an empty line to make it pretty
            System.out.println();

            if (conn != null) {
                System.out.println("Adding data to Rya repository...");
                // Dump model into Rya repository and check if we can query it.
                final RyaDAO<?> ryaDao = ExampleUtils.getRyaDaoAndPopulateWithModel(conn, m);
                final RyaStatement ryaStatement = createRyaStatement("http://www.Department0.University0.edu/FullProfessor1", null, null);

                // Check that statement exist in RyaDAO
                final List<RyaStatement> results = ExampleUtils.queryRyaStatement(ryaDao, ryaStatement);
                if (results.isEmpty()) {
                    System.out.println("No query results return from Rya for example: " + this.getClass().getSimpleName());
                }
            }
        }
    }

    public static void main(final String[] args) throws Exception {
        final SPARQLDLExample app = new SPARQLDLExample(null);
        app.run();
    }
}
