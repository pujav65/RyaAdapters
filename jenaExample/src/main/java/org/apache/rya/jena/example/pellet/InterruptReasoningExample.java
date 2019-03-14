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
package org.apache.rya.jena.example.pellet;

import static org.apache.rya.jena.example.pellet.util.ExampleUtils.createRyaStatement;

import java.util.List;

import org.apache.rya.api.domain.RyaStatement;
import org.apache.rya.jena.example.pellet.util.ExampleUtils;
import org.apache.rya.mongodb.MongoDBRyaDAO;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.examples.InterruptReasoningExample.WINE;
import org.mindswap.pellet.exceptions.TimeoutException;
import org.mindswap.pellet.jena.PelletInfGraph;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.mindswap.pellet.utils.Timers;
import org.openrdf.repository.sail.SailRepositoryConnection;

import com.clarkparsia.pellet.sparqldl.jena.SparqlDLExecutionFactory;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description: an example program that shows how different reasoning services
 * provided by Pellet can be interrupted based on a user-defined timeout without
 * affecting subsequent query results. The program defines some constant values for
 * timeout definitions to demonstrate various different things but results may vary
 * on different computers based on CPU speed, available memory, etc.
 * </p>
 * <p>
 * Sample output from this program looks like this:
 * <pre>
 * Parsing the ontology...finished
 *
 * Consistency Timeout: 5000ms
 * Checking consistency...finished in 1965
 *
 * Classify Timeout: 50000ms
 * Classifying...finished in 12668ms
 * Classified: true
 *
 * Realize Timeout: 1000ms
 * Realizing...interrupted after 1545ms
 * Realized: false
 *
 * Query Timeout: 0ms
 * Retrieving instances of AmericanWine...completed in 484ms (24 results)
 * Running SPARQL query...completed in 11801ms (23 results)
 *
 * Query Timeout: 200ms
 * Retrieving instances of AmericanWine...interrupted after 201ms
 * Running SPARQL query...interrupted after 201ms
 *
 * Query Timeout: 2000ms
 * Retrieving instances of AmericanWine...completed in 417ms (24 results)
 * Running SPARQL query...interrupted after 2001ms
 *
 * Query Timeout: 20000ms
 * Retrieving instances of AmericanWine...completed in 426ms (24 results)
 * Running SPARQL query...completed in 11790ms (23 results)
 * </pre>
 *
 * </p>
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 */
public class InterruptReasoningExample implements PelletExampleRunner {
    private static final String FILE = "file:src/main/resources/data/wine.owl";

    // various different constants to control the timeout values. typically
    // it is desirable to set different timeouts for classification and realization
    // since they are done only once and take more time compared to answering
    // queries
    public static class Timeouts {
        // timeout for consistency checking
        public static int CONSISTENCY = 5000;

        // timeout for classification
        public static int CLASSIFY = 50000;

        // timeout for realization (this value is intentionally
        // set to a unrealistically small value for demo purposes)
        public static int REALIZE = 1000;
    }

    // some constants related to wine ontology including some
    // arbitrary
    public static class Wine {
        public static final String NS = "http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#";

        public static final Resource AMERICAN_WINE = ResourceFactory.createResource( NS + "AmericanWine" );

        public static final Query QUERY =
            QueryFactory.create(
                "PREFIX wine: <" + Wine.NS + ">\n" +
                "SELECT * WHERE {\n" +
                "   ?x a wine:RedWine ; \n" +
                "      wine:madeFromGrape ?y \n" + "}" );
    }

    // the Jena model we are using
    private OntModel model;

    // underlying pellet graph
    private PelletInfGraph pellet;

    // the timers associated with the Pellet KB
    private Timers timers;

    @Override
    public void run() throws Exception {
        PelletOptions.USE_CLASSIFICATION_MONITOR = PelletOptions.MonitorType.NONE;

        // create the Jena model
        model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );

        pellet = (PelletInfGraph) model.getGraph();

        // get the underlying Pellet timers
        timers = pellet.getKB().timers;

        // set the timeout for main reasoning tasks
        timers.createTimer( "complete" ).setTimeout( Timeouts.CONSISTENCY );
        timers.createTimer( "classify" ).setTimeout( Timeouts.CLASSIFY );
        timers.createTimer( "realize" ).setTimeout( Timeouts.REALIZE );

        parse();

        consistency();

        classify();

        realize();

        query();

        timers.mainTimer.stop();
        if (conn != null) {
            System.out.println("Adding data to Rya repository...");
            // Dump model into Rya repository and check if we can query it.
            final MongoDBRyaDAO ryaDao = (MongoDBRyaDAO) ExampleUtils.getRyaDaoAndPopulateWithModel(conn, model);
            final RyaStatement ryaStatement = createRyaStatement(null, "http://www.w3.org/2002/07/owl#hasValue", "http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#Red");

            // Check that statement exist in RyaDAO
            final List<RyaStatement> results = ExampleUtils.queryRyaStatement(ryaDao, ryaStatement);
            if (results.isEmpty()) {
                System.out.println("No query results return from Rya for example: " + this.getClass().getSimpleName());
            }
        }
    }

    @Override
    public void cleanUp() throws Exception{
        PelletExampleRunner.super.cleanUp();
        PelletOptions.USE_CLASSIFICATION_MONITOR = PelletOptions.MonitorType.CONSOLE;
    }

    public static void main(final String[] args) throws Exception {
        final InterruptReasoningExample app = new InterruptReasoningExample(null);
        app.run();
    }

    private final SailRepositoryConnection conn;

    /**
     * Creates a new instance of {@link InterruptReasoningExample}.
     * @param conn the {@link SailRepositoryConnection}.
     */
    public InterruptReasoningExample(final SailRepositoryConnection conn) {
        this.conn = conn;
    }

    // read the data into a Jena model
    public void parse() {
        System.out.print( "Parsing the ontology..." );

        model.read(FILE);

        System.out.println( "finished" );
        System.out.println();
    }

    // check the consistency of data. this function will throw a TimeoutException
    // we don't catch the exception here because there is no point in continuing
    // if the initial consistency check is not finished. Pellet will not be able
    // to perform any reasoning steps if it dannot check the consistency.
    public void consistency() throws TimeoutException {
        System.out.println( "Consistency Timeout: " + Timeouts.CONSISTENCY + "ms" );
        System.out.print( "Checking consistency..." );

        model.prepare();

        System.out.println( "finished in " + timers.getTimer( "isConsistent").getLast() );
        System.out.println();
    }

    // classify the ontology
    public void classify() {
        System.out.println( "Classify Timeout: " + Timeouts.CLASSIFY + "ms" );
        System.out.print( "Classifying..." );

        try{
            ((PelletInfGraph) model.getGraph()).classify();
            System.out.println( "finished in " + timers.getTimer( "classify" ).getLast() + "ms" );
        } catch( final TimeoutException e ) {
            System.out.println( "interrupted after " + timers.getTimer( "classify" ).getElapsed() + "ms" );
        }

        System.out.println( "Classified: " + pellet.isClassified()  );
        System.out.println();
    }

    public void realize() {
        // realization can only be done if classification is completed
        if( !pellet.isClassified() ) {
            return;
        }

        System.out.println( "Realize Timeout: " + Timeouts.REALIZE + "ms" );
        System.out.print( "Realizing..." );

        try{
            pellet.realize();
            System.out.println( "finished in " + timers.getTimer( "realize" ).getLast() + "ms" );
        } catch( final TimeoutException e ) {
            System.out.println( "interrupted after " + timers.getTimer( "realize" ).getElapsed() + "ms" );
        }

        System.out.println( "Realized: " + pellet.isRealized()  );
        System.out.println();
    }

    // run some sample queries with different timeouts
    public void query() throws Exception {
        // different timeout values in ms for querying (0 means no timeout)
        final int[] timeouts = { 0, 200, 2000, 20000 };

        for( final int timeout : timeouts ) {
            // update the timeout value
            timers.mainTimer.setTimeout( timeout );
            System.out.println( "Query Timeout: " + timeout + "ms" );

            // run the queries
            getInstances( WINE.AmericanWine );
            execQuery( WINE.query );

            System.out.println();
        }
    }

    public void getInstances(final Resource cls) {
        System.out.print( "Retrieving instances of " + cls.getLocalName() + "..." );

        // we need to restart the timer every time because timeouts are checked
        // w.r.t. the time a timer was started. not resetting the timer will
        // cause timeout exceptions nearly all the time
        timers.mainTimer.restart();

        try {
            // run a simple query using Jena interface
            final ExtendedIterator<Individual> results = model.listIndividuals( cls );

            // print if the query succeeded
            final int size = results.toList().size();
            System.out.print( "completed in " + timers.mainTimer.getElapsed() + "ms" );
            System.out.println(" (" + size + " results)" );
        } catch( final TimeoutException e ) {
            System.out.println( "interrupted after " + timers.mainTimer.getElapsed() + "ms" );
        }
    }

    public void execQuery(final Query query) throws Exception {
        System.out.print( "Running SPARQL query..." );

        // we need to restart the timer as above
        timers.mainTimer.restart();

        try {
            // run the SPARQL query
            final ResultSet results = SparqlDLExecutionFactory.create( query, model ).execSelect();

            final int size = ResultSetFormatter.consume( results );
            System.out.print( "completed in " + timers.mainTimer.getElapsed() + "ms" );
            System.out.println(" (" + size + " results)" );
        } catch( final TimeoutException e ) {
            System.out.println( "interrupted after " + timers.mainTimer.getElapsed() + "ms" );
        }
    }
}
