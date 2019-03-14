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
package org.apache.rya.adapter.rest.reasoner.types.jena;

import static org.apache.rya.adapter.rest.reasoner.helper.StatementUtils.createStatement;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.rya.adapter.rest.reasoner.types.ReasonerResult;
import org.apache.rya.api.RdfCloudTripleStoreConfiguration;
import org.apache.rya.api.domain.RyaStatement;
import org.apache.rya.indexing.accumulo.ConfigUtils;
import org.apache.rya.mongodb.MongoDBRdfConfiguration;
import org.apache.rya.mongodb.MongoITBase;
import org.apache.rya.sail.config.RyaSailFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.Sail;

/**
 * Tests the methods of {@link JenaReasonerRunner}.
 */
public class JenaReasonerRunnerTest extends MongoITBase {
    private SailRepository repository = null;

    @Before
    public void setup() throws Exception {
        final Configuration conf = super.conf;

        final Sail sail = RyaSailFactory.getInstance(conf);
        repository = new SailRepository(sail);
    }

    @After
    public void shutdown() throws Exception {
        if (repository != null) {
            repository.shutDown();
        }
    }

    @Override
    public void updateConfiguration(final MongoDBRdfConfiguration conf) {
        conf.setBoolean(ConfigUtils.DISPLAY_QUERY_PLAN, true);
        conf.setBoolean(RdfCloudTripleStoreConfiguration.CONF_INFER, true);
        conf.setStrings(ConfigUtils.FREETEXT_PREDICATES_LIST, new String[] {RDFS.LABEL.stringValue()});
        conf.setBoolean(ConfigUtils.USE_FREETEXT, true);
    }

    @Test
    public void testJenaReasonerRunner() throws Exception {
        final JenaReasonerRunner jenaReasonerRunner = new JenaReasonerRunner();

        final String namespace = "http://rya.apache.org/jena/ns/sports#";
        final Statement stmt1 = createStatement("Bob", "plays", "Football", namespace);
        final Statement stmt2 = createStatement("Susan", "coaches", "Football", namespace);
        final Statement inferredStmt = createStatement("Susan", "hasPlayer", "Bob", namespace);

        RepositoryConnection addConnection = null;
        try {
            // Load some data.
            addConnection = repository.getConnection();

            addConnection.add(stmt1);
            addConnection.add(stmt2);
        } finally {
            if (addConnection != null) {
                addConnection.close();
            }
        }

        final String filename = "src/test/resources/rdf_format_files/notation3_files/rule_files/n3_rules.txt";
        final ReasonerResult reasonerResult = jenaReasonerRunner.runReasoner(repository, filename);

        assertNotNull(reasonerResult);
        final List<RyaStatement> ryaStatements = reasonerResult.getRyaStatements();
        assertNotNull(ryaStatements);
        assertEquals(1, ryaStatements.size());

        final Resource s = inferredStmt.getSubject();
        final URI p = inferredStmt.getPredicate();
        final Value o = inferredStmt.getObject();

        RepositoryConnection conn = null;
        try {
            conn = repository.getConnection();

            final RepositoryResult<Statement> resultsIter = conn.getStatements(s, p, o, true);

            // Get the statement.
            assertTrue(resultsIter.hasNext());
            final Statement resultStatement = resultsIter.next();
            assertNotNull(resultStatement);
            assertNotNull(resultStatement.getSubject());
            assertNotNull(resultStatement.getPredicate());
            assertNotNull(resultStatement.getObject());
            assertEquals(inferredStmt.getSubject(), resultStatement.getSubject());
            assertEquals(inferredStmt.getPredicate(), resultStatement.getPredicate());
            assertEquals(inferredStmt.getObject(), resultStatement.getObject());

            // There should be only 1 statement.
            assertFalse(resultsIter.hasNext());
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }
}