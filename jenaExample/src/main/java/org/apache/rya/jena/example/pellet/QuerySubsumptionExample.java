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
// This source code is available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package org.apache.rya.jena.example.pellet;

import static org.apache.rya.jena.example.pellet.util.ExampleUtils.createRyaStatement;

import java.util.Iterator;
import java.util.List;

import org.apache.rya.api.domain.RyaStatement;
import org.apache.rya.jena.example.pellet.util.ExampleUtils;
import org.apache.rya.mongodb.MongoDBRyaDAO;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.jena.PelletInfGraph;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.openrdf.repository.sail.SailRepositoryConnection;

import com.clarkparsia.pellet.sparqldl.engine.QueryEngine;
import com.clarkparsia.pellet.sparqldl.engine.QuerySubsumption;
import com.clarkparsia.pellet.sparqldl.model.Query;
import com.clarkparsia.pellet.sparqldl.model.QueryResult;
import com.clarkparsia.pellet.sparqldl.model.ResultBinding;
import com.clarkparsia.pellet.sparqldl.parser.QueryParser;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import aterm.ATermAppl;

/**
 * Example program to demonstrate the query subsumption (query containment)
 * capabilities of Pellet. Query subsumption service reports if the answers to a
 * query would be contained in the answers of another query. It is similar to
 * concept subsumption service but applies to conjunctive queries. The examples
 * in this sample program show both concept subsumption and concept equivalence
 * services. The examples also show how to get the mapping between query
 * variables if the subsumption holds.
 */
public class QuerySubsumptionExample implements PelletExampleRunner {
    private static final String ONT = "file:src/main/resources/data/family.owl";
    private static final String FAMILY    = "http://www.example.org/family#";
    private static final String PREFIX    = "PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\r\n"
                                    + "PREFIX family: <" + FAMILY + ">\r\n" + "SELECT * { ";
    private static final String SUFFIX    = " }";

    public static void main(final String[] args) throws Exception {
        final QuerySubsumptionExample app = new QuerySubsumptionExample(null);
        app.run();
    }

    private KnowledgeBase kb;
    private QueryParser parser;
    private final SailRepositoryConnection conn;

    /**
     * Creates a new instance of {@link QuerySubsumptionExample}.
     * @param conn the {@link SailRepositoryConnection}.
     */
    public QuerySubsumptionExample(final SailRepositoryConnection conn) {
        this.conn = conn;
    }

    public Query query(final String queryStr) {
        return parser.parse( PREFIX + queryStr + SUFFIX, kb );
    }

    @Override
    public void run() throws Exception {
        run(ONT);
    }

    public void run(final String file) throws Exception {
        final OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        model.read( file );
        model.prepare();

        ExampleUtils.printModelStatements(model);

        kb = ((PelletInfGraph) model.getGraph()).getKB();
        parser = QueryEngine.getParser();

        example1();

        example2();

        example3();

        example4();

        if (conn != null) {
            System.out.println("Adding data to Rya repository...");
            // Dump model into Rya repository and check if we can query it.
            final MongoDBRyaDAO ryaDao = (MongoDBRyaDAO) ExampleUtils.getRyaDaoAndPopulateWithModel(conn, model);
            final RyaStatement ryaStatement = createRyaStatement(FAMILY + "father", null, null);

            // Check that statement exist in RyaDAO
            final List<RyaStatement> results = ExampleUtils.queryRyaStatement(ryaDao, ryaStatement);
            if (results.isEmpty()) {
                System.out.println("No query results return from Rya for example: " + this.getClass().getSimpleName());
            }
        }
    }

    /**
     * Simple query subsumption similar to standard concept subsumption. Every
     * Male is a Person so query 1 is subsumed by query 2. The converse is
     * obviously not true.
     */
    public void example1() {
        final Query q1 = query( "?x a family:Male ." );
        final Query q2 = query( "?x a family:Person ." );

        System.out.println( "Example 1" );
        System.out.println( "=========" );
        System.out.println( "Query 1: " + q1.toString());
        System.out.println( "Query 2: " + q2.toString() );
        System.out.println();
        System.out.println( "Query 1 is subsumed by query 2: " + QuerySubsumption.isSubsumedBy( q1, q2 ) );
        System.out.println( "Query 2 is subsumed by query 1: " + QuerySubsumption.isSubsumedBy( q2, q1 ) );
        System.out.println();
    }

    /**
     * Another example of subsumption. First query asks for all people married
     * to Male individuals which is subsumed by the second query which asks for
     * all Females.
     */
    public void example2() {
        final Query q3 = query( "?x family:isMarriedTo ?y . ?y rdf:type family:Male" );
        final Query q4 = query( "?x a family:Female ." );

        System.out.println( "Example 2" );
        System.out.println( "=========" );
        System.out.println( "Query 3: " + q3.toString() );
        System.out.println( "Query 4: " + q4.toString() );
        System.out.println();
        System.out.println( "Query 3 is subsumed by query 4: " + QuerySubsumption.isSubsumedBy( q3, q4 ) );
        System.out.println( "Query 4 is subsumed by query 3: " + QuerySubsumption.isSubsumedBy( q4, q3 ) );
        System.out.println();
    }

    /**
     * Example showing query equivalence. The subproperty relation between
     * hasFather and hasParent properties would normally establish subsumption
     * in one way but due to cardinality restrictions defined in the ontology
     * two queries end up being equivalent,
     */
    public void example3() {
        final Query q5 = query( "?x family:hasFather ?y . " );
        final Query q6 = query( "?x family:hasParent ?y . ?y a family:Male ." );

        System.out.println( "Example 3" );
        System.out.println( "=========" );
        System.out.println( "Query 5: " + q5.toString() );
        System.out.println( "Query 6: " + q6.toString() );
        System.out.println();
        System.out.println( "Query 5 is subsumed by query 6: " + QuerySubsumption.isSubsumedBy( q5, q6 ) );
        System.out.println( "Query 6 is subsumed by query 5: " + QuerySubsumption.isSubsumedBy( q5, q6 ) );

        System.out.println( "Query 5 is equivalent to query 6: " + QuerySubsumption.isEquivalentTo( q5, q6 ) );
        System.out.println();
    }

    /**
     * The subsumption in this example holds because of the subproperty relation
     * between hasBrother and hasSibling. however, The second query uses the
     * variable name ?z instead of the the variable name ?y used in the first
     * query. The query subsumption algorithm finds the mapping between query
     * variables.
     */
    public void example4() {
        final Query q7 = query( "?x a family:Female; family:hasBrother ?y . " );
        final Query q8 = query( "?x a family:Female; family:hasSibling ?z ." );

        System.out.println( "Example 4" );
        System.out.println( "=========" );
        System.out.println( "Query 7: " + q7.toString() );
        System.out.println( "Query 8: " + q8.toString() );
        System.out.println();
        System.out.println( "Query 7 is subsumed by query 8: " + QuerySubsumption.isSubsumedBy( q7, q8 ) );

        System.out.print( "Subsumption mappings: " );
        final QueryResult mappings = QuerySubsumption.getSubsumptionMappings( q7, q8 );
        for (final ResultBinding mapping : mappings) {
            for( final Iterator<?> j = q8.getVars().iterator(); j.hasNext(); ) {
                final ATermAppl var = (ATermAppl) j.next();
                System.out.print( var.getArgument( 0 ) + " -> " + mapping.getValue( var )); //I get var(x) as opposed to x
                if( j.hasNext() ) {
                    System.out.print( ", " );
                }
            }
        }
        System.out.println();
    }
}
