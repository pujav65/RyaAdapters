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

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.rya.adapter.rest.reasoner.types.ReasonerResult;
import org.apache.rya.api.RdfCloudTripleStoreConfiguration;
import org.apache.rya.api.domain.RyaStatement;
import org.apache.rya.indexing.GeoConstants;
import org.apache.rya.indexing.accumulo.ConfigUtils;
import org.apache.rya.mongodb.MockMongoFactory;
import org.apache.rya.mongodb.MongoConnectorFactory;
import org.apache.rya.mongodb.MongoDBRdfConfiguration;
import org.apache.rya.sail.config.RyaSailFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.sail.Sail;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

/**
 * Tests the methods of {@link JenaReasonerRunner}.
 */
public class JenaReasonerRunnerTest {
    private static final boolean PRINT_QUERIES = true;
    private static final String MONGO_DB = "rya";
    private static final String MONGO_COLL_PREFIX = "rya_";
    private static final boolean USE_MOCK = true;
    private static final boolean USE_INFER = true;
    private static final String MONGO_INSTANCE_URL = "localhost";
    private static final String MONGO_INSTANCE_PORT = "27017";

    private static SailRepository repository = null;
    private static SailRepositoryConnection conn;
    private static MockMongoFactory mock = null;

    @BeforeClass
    public static void setup() throws Exception {
        final Configuration conf = getConf();
        conf.setBoolean(ConfigUtils.DISPLAY_QUERY_PLAN, PRINT_QUERIES);

        final Sail sail = RyaSailFactory.getInstance(conf);
        repository = new SailRepository(sail);
        conn = repository.getConnection();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        if (conn != null) {
            conn.close();
        }
        if (repository != null) {
            repository.shutDown();
        }
        if (mock != null) {
            mock.shutdown();
        }
        MongoConnectorFactory.closeMongoClient();
    }

    private static Configuration getConf() throws IOException {
        final MongoDBRdfConfiguration conf = new MongoDBRdfConfiguration();
        conf.set(ConfigUtils.USE_MONGO, "true");

        if (USE_MOCK) {
            mock = MockMongoFactory.newFactory();
            final MongoClient c = mock.newMongoClient();
            final ServerAddress address = c.getAddress();
            final String url = address.getHost();
            final String port = Integer.toString(address.getPort());
            c.close();
            conf.set(MongoDBRdfConfiguration.MONGO_INSTANCE, url);
            conf.set(MongoDBRdfConfiguration.MONGO_INSTANCE_PORT, port);
        } else {
            // User name and password must be filled in:
            conf.set(MongoDBRdfConfiguration.MONGO_USER, "fill this in");
            conf.set(MongoDBRdfConfiguration.MONGO_USER_PASSWORD, "fill this in");
            conf.set(MongoDBRdfConfiguration.MONGO_INSTANCE, MONGO_INSTANCE_URL);
            conf.set(MongoDBRdfConfiguration.MONGO_INSTANCE_PORT, MONGO_INSTANCE_PORT);
        }
        conf.set(MongoDBRdfConfiguration.MONGO_DB_NAME, MONGO_DB);
        conf.set(MongoDBRdfConfiguration.MONGO_COLLECTION_PREFIX, MONGO_COLL_PREFIX);
        conf.set(ConfigUtils.GEO_PREDICATES_LIST, "http://www.opengis.net/ont/geosparql#asWKT");
//        conf.set(ConfigUtils.USE_GEO, "true");
        conf.set(ConfigUtils.USE_FREETEXT, "true");
        conf.setTablePrefix(MONGO_COLL_PREFIX);
        conf.set(ConfigUtils.GEO_PREDICATES_LIST, GeoConstants.GEO_AS_WKT.stringValue());
        conf.set(ConfigUtils.FREETEXT_PREDICATES_LIST, RDFS.LABEL.stringValue());
        conf.set(ConfigUtils.FREETEXT_PREDICATES_LIST, RDFS.LABEL.stringValue());
        conf.set(RdfCloudTripleStoreConfiguration.CONF_INFER, Boolean.toString(USE_INFER));
        return conf;
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
        final ReasonerResult reasonerResult = jenaReasonerRunner.runReasoner(conn, filename);

        assertNotNull(reasonerResult);
        final List<RyaStatement> ryaStatements = reasonerResult.getRyaStatements();
        assertNotNull(ryaStatements);
        assertEquals(1, ryaStatements.size());

        final Resource s = inferredStmt.getSubject();
        final URI p = inferredStmt.getPredicate();
        final Value o = inferredStmt.getObject();

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
    }
}