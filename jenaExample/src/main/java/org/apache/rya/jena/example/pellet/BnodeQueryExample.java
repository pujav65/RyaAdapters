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
// Portions Copyright (c) 2006 - 2008, Clark & Parsia, LLC. <http://www.clarkparsia.com>
// Clark & Parsia, LLC parts of this source code are available under the terms of the Affero General Public License v3.
//
// Please see LICENSE.txt for full license terms, including the availability of proprietary exceptions.
// Questions, comments, or requests for clarification: licensing@clarkparsia.com

package org.apache.rya.jena.example.pellet;

import static org.apache.rya.jena.example.pellet.util.ExampleUtils.createRyaStatement;

import java.util.List;

import org.apache.rya.api.domain.RyaStatement;
import org.apache.rya.jena.example.pellet.util.ExampleUtils;
import org.apache.rya.mongodb.MongoDBRyaDAO;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.openrdf.repository.sail.SailRepositoryConnection;

import com.clarkparsia.pellet.sparqldl.jena.SparqlDLExecutionFactory;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Bnodes used in a SPARQL query are treated as variables but different from ordinary
 * SPARQL variables they are not required to be bound to existing individuals. This means
 * a bnode in the query can be matched with an individual whose existence is inferred from
 * a someValues or a minCardinality restriction.
 */
public class BnodeQueryExample implements PelletExampleRunner {
    private static final String FILE = "file:src/main/resources/data/wine.owl";
    private static final String NS = "http://www.w3.org/TR/2003/PR-owl-guide-20031209/food#";

    private final SailRepositoryConnection conn;

    /**
     * Creates a new instance of {@link BnodeQueryExample}.
     * @param conn the {@link SailRepositoryConnection}.
     */
    public BnodeQueryExample(final SailRepositoryConnection conn) {
        this.conn = conn;
    }

    @Override
    public void run() throws Exception {
        run(FILE);
    }

    public void run(final String file) throws Exception {
        PelletOptions.TREAT_ALL_VARS_DISTINGUISHED = false;

        // create an empty ontology model using Pellet spec
        final OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );

        // read the file
        model.read(file);

        // getOntClass is not used because of the same reason mentioned above
        // (i.e. avoid unnecessary classifications)
        final Resource RedMeatCourse = model.getResource( NS + "RedMeatCourse" );
        final Resource PastaWithLightCreamCourse = model.getResource( NS + "PastaWithLightCreamCourse" );

        // create two individuals Lunch and dinner that are instances of
        // PastaWithLightCreamCourse and RedMeatCourse, respectively
        model.createIndividual( NS + "MyLunch", PastaWithLightCreamCourse);
        model.createIndividual( NS + "MyDinner", RedMeatCourse);

        final String queryBegin =
            "PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\r\n" +
            "PREFIX food: <http://www.w3.org/TR/2003/PR-owl-guide-20031209/food#>\r\n" +
            "PREFIX wine: <http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#>\r\n" +
            "\r\n" +
            "SELECT ?Meal ?WineColor\r\n" +
            "WHERE {\r\n";
        final String queryEnd = "}";

        // create a query that asks for the color of the wine that
        // would go with each meal course
        final String queryStr1 =
            queryBegin +
            "   ?Meal rdf:type food:MealCourse .\r\n" +
            "   ?Meal food:hasDrink _:Wine .\r\n" +
            "   _:Wine wine:hasColor ?WineColor" +
            queryEnd;

        // same query as above but uses a variable instead of a bnode
        final String queryStr2 =
            queryBegin +
            "   ?Meal rdf:type food:MealCourse .\r\n" +
            "   ?Meal food:hasDrink ?Wine .\r\n" +
            "   ?Wine wine:hasColor ?WineColor" +
            queryEnd;

        final Query query1 = QueryFactory.create( queryStr1 );
        final Query query2 = QueryFactory.create( queryStr2 );

        // The following definitions from food ontology dictates that
        // PastaWithLightCreamCourse has white wine and RedMeatCourse
        // has red wine.
        //  Class(PastaWithLightCreamCourse partial
        //        restriction(hasDrink allValuesFrom(restriction(hasColor value (White)))))
        //  Class(RedMeatCourse partial
        //        restriction(hasDrink allValuesFrom(restriction(hasColor value (Red)))))
        //
        // PelletQueryEngine will successfully find the answer for the first query
        printQueryResults(
            "Running first query with PelletQueryEngine...",
            SparqlDLExecutionFactory.create( query1, model ), query1 );

        // The same query (with variables instead of bnodes) will not return same answers!
        // The reason is this: In the second query we are using a variable that needs to be
        // bound to a specific wine instance. The reasoner knows that there is a wine (due
        // to the cardinality restriction in the ontology) but does not know the URI for
        // that individual. Therefore, query fails because no binding can be found.
        //
        // Note that this behavior is similar to what you get with "must-bind", "don't-bind"
        // variables in OWL-QL. In this case, variables in the query are "must-bind" variables
        // and bnodes are "don't-bind" variables.
        printQueryResults(
            "Running second query with PelletQueryEngine...",
            SparqlDLExecutionFactory.create( query2, model ), query2 );

        // When the standard QueryEngine of Jena is used we don't get the results even
        // for the first query. The reason is Jena QueryEngine evaluates the query one triple
        // at a time and thus fails when the wine individual is not found. PelletQueryEngine
        // evaluates the query as a whole and succeeds. (If the above feature, creating bnodes
        // automatically, is added to Pellet then you would get the same results here)
        printQueryResults(
            "Running first query with standard Jena QueryEngine...",
            QueryExecutionFactory.create( query1, model ), query1 );

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
    public void cleanUp() throws Exception {
        PelletExampleRunner.super.cleanUp();
        PelletOptions.TREAT_ALL_VARS_DISTINGUISHED = true;
    }

    public static void printQueryResults( final String header, final QueryExecution qe, final Query query ) throws Exception {
        System.out.println(header);

        final ResultSet results = qe.execSelect();

        ResultSetFormatter.out( System.out, results, query );

        System.out.println();
    }

    public static void main(final String[] args) throws Exception {
        final BnodeQueryExample app = new BnodeQueryExample(null);
        app.run();
    }
}
