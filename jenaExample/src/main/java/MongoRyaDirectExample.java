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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.Logger;
import org.apache.rya.api.RdfCloudTripleStoreConfiguration;
import org.apache.rya.api.domain.RyaStatement;
import org.apache.rya.api.persist.RyaDAO;
import org.apache.rya.api.persist.RyaDAOException;
import org.apache.rya.api.persist.query.RyaQuery;
import org.apache.rya.api.resolver.RdfToRyaConversions;
import org.apache.rya.indexing.GeoConstants;
import org.apache.rya.indexing.accumulo.ConfigUtils;
import org.apache.rya.jena.jenasesame.JenaSesame;
import org.apache.rya.mongodb.MockMongoFactory;
import org.apache.rya.mongodb.MongoConnectorFactory;
import org.apache.rya.mongodb.MongoDBRdfConfiguration;
import org.apache.rya.mongodb.MongoDBRyaDAO;
import org.apache.rya.rdftriplestore.RdfCloudTripleStore;
import org.apache.rya.sail.config.RyaSailFactory;
import org.calrissian.mango.collect.CloseableIterable;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryResultHandlerException;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.sail.Sail;

import com.google.common.base.Charsets;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.reasoner.rulesys.Rule.Parser;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

public class MongoRyaDirectExample {
    private static final Logger log = Logger.getLogger(MongoRyaDirectExample.class);

    //
    // Connection configuration parameters
    //

    private static final boolean PRINT_QUERIES = true;
    private static final String MONGO_DB = "rya";
    private static final String MONGO_COLL_PREFIX = "rya_";
    private static final boolean USE_MOCK = true;
    private static final boolean USE_INFER = true;
    private static final String MONGO_INSTANCE_URL = "localhost";
    private static final String MONGO_INSTANCE_PORT = "27017";

    public static void main(final String[] args) throws Exception {
        final Configuration conf = getConf();
        conf.setBoolean(ConfigUtils.DISPLAY_QUERY_PLAN, PRINT_QUERIES);

        SailRepository repository = null;
        SailRepositoryConnection conn = null;
        try {
            log.info("Connecting to Indexing Sail Repository.");
            final Sail sail = RyaSailFactory.getInstance(conf);
            repository = new SailRepository(sail);
            conn = repository.getConnection();

            final long start = System.currentTimeMillis();
            log.info("Running SPARQL Example: Add and Delete");
//            testAddPointAndWithinSearch(conn);
           //  to test out inference, set inference to true in the conf

            log.info("Running Jena Sesame Reasoning with Rules Example");
            testJenaSesameReasoningWithRules(conn);

            log.info("TIME: " + (System.currentTimeMillis() - start) / 1000.);
        } catch(final Exception e) {
            log.error("Encountered error running MongoDB example", e);
        } finally {
            log.info("Shutting down");
            closeQuietly(conn);
            closeQuietly(repository);
            if (mock != null) {
                mock.shutdown();
            }
            MongoConnectorFactory.closeMongoClient();
        }
    }

//    private static void testAddPointAndWithinSearch(SailRepositoryConnection conn) throws Exception {
//
//        String update = "PREFIX geo: <http://www.opengis.net/ont/geosparql#>  "//
//                + "INSERT DATA { " //
//                + "  <urn:feature> a geo:Feature ; " //
//                + "    geo:hasGeometry [ " //
//                + "      a geo:Point ; " //
//                + "      geo:asWKT \"Point(-77.03524 38.889468)\"^^geo:wktLiteral "//
//                + "    ] . " //
//                + "}";
//
//        Update u = conn.prepareUpdate(QueryLanguage.SPARQL, update);
//        u.execute();
//
//        String queryString;
//        TupleQuery tupleQuery;
//        CountingResultHandler tupleHandler;
//
//        // ring containing point
//        queryString = "PREFIX geo: <http://www.opengis.net/ont/geosparql#>  "//
//                + "PREFIX geof: <http://www.opengis.net/def/function/geosparql/>  "//
//                + "SELECT ?feature ?point ?wkt " //
//                + "{" //
//                + "  ?feature a geo:Feature . "//
//                + "  ?feature geo:hasGeometry ?point . "//
//                + "  ?point a geo:Point . "//
//                + "  ?point geo:asWKT ?wkt . "//
//                + "  FILTER(geof:sfWithin(?wkt, \"POLYGON((-78 39, -77 39, -77 38, -78 38, -78 39))\"^^geo:wktLiteral)) " //
//                + "}";//
//        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
//
//        tupleHandler = new CountingResultHandler();
//        tupleQuery.evaluate(tupleHandler);
//        log.info("Result count : " + tupleHandler.getCount());
//        Validate.isTrue(tupleHandler.getCount() >= 1); // may see points from during previous runs
//
//        // ring outside point
//        queryString = "PREFIX geo: <http://www.opengis.net/ont/geosparql#>  "//
//                + "PREFIX geof: <http://www.opengis.net/def/function/geosparql/>  "//
//                + "SELECT ?feature ?point ?wkt " //
//                + "{" //
//                + "  ?feature a geo:Feature . "//
//                + "  ?feature geo:hasGeometry ?point . "//
//                + "  ?point a geo:Point . "//
//                + "  ?point geo:asWKT ?wkt . "//
//                + "  FILTER(geof:sfWithin(?wkt, \"POLYGON((-77 39, -76 39, -76 38, -77 38, -77 39))\"^^geo:wktLiteral)) " //
//                + "}";//
//        tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
//
//        tupleHandler = new CountingResultHandler();
//        tupleQuery.evaluate(tupleHandler);
//        log.info("Result count : " + tupleHandler.getCount());
//        Validate.isTrue(tupleHandler.getCount() == 0);
//    }

    private static void closeQuietly(final SailRepository repository) {
        if (repository != null) {
            try {
                repository.shutDown();
            } catch (final RepositoryException e) {
                // quietly absorb this exception
            }
        }
    }

    private static void closeQuietly(final SailRepositoryConnection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (final RepositoryException e) {
                // quietly absorb this exception
            }
        }
    }

    private static MockMongoFactory mock = null;
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
    protected static StatementImpl createStatement(final String subject, final String predicate, final String object) {
        return new StatementImpl(new URIImpl(subject), new URIImpl(predicate), new URIImpl(object));
    }

    protected static StatementImpl createStatement(final String subject, final String predicate, final String object, final String namespace) {
        return createStatement(namespace + subject, namespace + predicate, namespace + object);
}
    private static void testJenaSesameReasoningWithRules(final SailRepositoryConnection conn) throws Exception {
        final Repository repo = conn.getRepository();
        RepositoryConnection addConnection = null;
        RepositoryConnection queryConnection = null;
        try {
            // Load some data.
            addConnection = repo.getConnection();

            final String namespace = "http://rya.apache.org/jena/ns/sports#";
            final Statement stmt1 = createStatement("Bob", "plays", "Football", namespace);
            final Statement stmt2 = createStatement("Susan", "coaches", "Football", namespace);
            final Statement inferredStmt = createStatement("Susan", "hasPlayer", "Bob", namespace);

            addConnection.add(stmt1);
            addConnection.add(stmt2);
            addConnection.close();

            queryConnection = repo.getConnection();

            final Dataset dataset = JenaSesame.createDataset(queryConnection);

            final Model model = dataset.getDefaultModel();
            log.info(model.getNsPrefixMap());

            final String ruleSource =
                "@prefix : <" + namespace + "> .\r\n" +
                "\r\n" +
                "[ruleHasPlayer: (?s :plays ?c) (?p :coaches ?c) -> (?p :hasPlayer ?s)]";

            Reasoner reasoner = null;
            try (
                final InputStream in = IOUtils.toInputStream(ruleSource, Charsets.UTF_8);
                final BufferedReader br = new BufferedReader(new InputStreamReader(in));
            ) {
                final Parser parser = Rule.rulesParserFromReader(br);
                reasoner = new GenericRuleReasoner(Rule.parseRules(parser));
            }

            final InfModel infModel = ModelFactory.createInfModel(reasoner, model);

            final StmtIterator iterator = infModel.listStatements();

            int count = 0;
            while (iterator.hasNext()) {
                final com.hp.hpl.jena.rdf.model.Statement stmt = iterator.nextStatement();

                final Resource subject = stmt.getSubject();
                final Property predicate = stmt.getPredicate();
                final RDFNode object = stmt.getObject();

                log.info(subject.toString() + " " + predicate.toString() + " " + object.toString());
                // TODO: Should inferred statements be added automatically?
                model.add(stmt);
                count++;
            }
            log.info("Result count : " + count);

            // Check that statements exist in MongoDBRyaDAO
            final SailRepository sailRepository = ((SailRepository)repo);
            final RdfCloudTripleStore rdfCloudTripleStore = ((RdfCloudTripleStore)sailRepository.getSail());
            final MongoDBRyaDAO ryaDao = (MongoDBRyaDAO) rdfCloudTripleStore.getRyaDAO();

            final RyaStatement ryaStmt1 = RdfToRyaConversions.convertStatement(stmt1);
            final RyaStatement ryaStmt2 = RdfToRyaConversions.convertStatement(stmt2);
            final RyaStatement inferredRyaStmt = RdfToRyaConversions.convertStatement(inferredStmt);

            Validate.isTrue(containsStatement(ryaStmt1, ryaDao));
            Validate.isTrue(containsStatement(ryaStmt2, ryaDao));
            Validate.isTrue(containsStatement(inferredRyaStmt, ryaDao));
        } catch (final Exception e) {
            log.error("Encountered an exception while running reasoner.", e);
        } finally {
            if (addConnection != null) {
                addConnection.close();
            }
            if (queryConnection != null) {
                queryConnection.close();
            }
        }
    }
    protected static boolean containsStatement(final RyaStatement ryaStatement, final RyaDAO<? extends RdfCloudTripleStoreConfiguration> ryaDao) throws RyaDAOException, IOException {
        final RyaQuery ryaQuery = new RyaQuery(ryaStatement);
        final CloseableIterable<RyaStatement> closeableIter = ryaDao.getQueryEngine().query(ryaQuery);
        int count = 0;
        try {
            final Iterator<RyaStatement> iterator = closeableIter.iterator();
            while (iterator.hasNext()) {
                iterator.next();
                count++;
            }
        } catch (final Exception e) {
            log.error("Error querying for statement", e);
        } finally {
            closeableIter.close();
        }

        return count > 0;
    }

    private static class CountingResultHandler implements TupleQueryResultHandler {
        private int count = 0;

        public int getCount() {
            return count;
        }

        public void resetCount() {
            this.count = 0;
        }

        @Override
        public void startQueryResult(final List<String> arg0) throws TupleQueryResultHandlerException {
        }

        @Override
        public void handleSolution(final BindingSet arg0) throws TupleQueryResultHandlerException {
            count++;
            System.out.println(arg0);
        }

        @Override
        public void endQueryResult() throws TupleQueryResultHandlerException {
        }

        @Override
        public void handleBoolean(final boolean arg0) throws QueryResultHandlerException {
          // TODO Auto-generated method stub

        }

        @Override
        public void handleLinks(final List<String> arg0) throws QueryResultHandlerException {
          // TODO Auto-generated method stub

        }
    }
}
