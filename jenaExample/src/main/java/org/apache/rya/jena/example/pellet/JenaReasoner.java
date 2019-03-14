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

import java.util.Iterator;
import java.util.List;

import org.apache.rya.api.domain.RyaStatement;
import org.apache.rya.jena.example.pellet.util.ExampleUtils;
import org.apache.rya.mongodb.MongoDBRyaDAO;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.openrdf.repository.sail.SailRepositoryConnection;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * An example to show how to use PelletReasoner as a Jena reasoner. The reasoner can
 * be directly attached to a plain RDF <code>Model</code> or attached to an <code>OntModel</code>.
 * This program shows how to do both of these operations and achieve the exact same results.
 */
public class JenaReasoner implements PelletExampleRunner {
    private static final String FILE = "file:src/main/resources/data/koala.owl";

    // ontology that will be used
    private static final String NS = "http://protege.stanford.edu/plugins/owl/owl-library/koala.owl#";

    private final SailRepositoryConnection conn;

    /**
     * Creates a new instance of {@link JenaReasoner}.
     * @param conn the {@link SailRepositoryConnection}.
     */
    public JenaReasoner(final SailRepositoryConnection conn) {
        this.conn = conn;
    }

    @Override
    public void run() throws Exception {
        run(FILE);
    }

    public void run(final String file) throws Exception {
        usageWithDefaultModel(file);

        usageWithOntModel(file);
    }

    public static void main(final String[] args) throws Exception {
        final JenaReasoner app = new JenaReasoner(null);
        app.run();
    }

    public void usageWithDefaultModel(final String file) throws Exception {
        System.out.println("Results with plain RDF Model");
        System.out.println("----------------------------");
        System.out.println();

          // create Pellet reasoner
        final Reasoner reasoner = PelletReasonerFactory.theInstance().create();

        // create an empty model
        final Model emptyModel = ModelFactory.createDefaultModel( );

        // create an inferencing model using Pellet reasoner
        final InfModel model = ModelFactory.createInfModel( reasoner, emptyModel );

        // read the file
        model.read( file );

        // print validation report
        final ValidityReport report = model.validate();
        printIterator( report.getReports(), "Validation Results" );

        // print superclasses
        final Resource c = model.getResource( NS + "MaleStudentWith3Daughters" );
        printIterator(model.listObjectsOfProperty(c, RDFS.subClassOf), "All super classes of " + c.getLocalName());

        System.out.println();

        if (conn != null) {
            System.out.println("Adding data to Rya repository...");
            // Dump model into Rya repository and check if we can query it.
            final MongoDBRyaDAO ryaDao = (MongoDBRyaDAO) ExampleUtils.getRyaDaoAndPopulateWithModel(conn, model);
            final RyaStatement ryaStatement = createRyaStatement(NS + "MaleStudentWith3Daughters", null, null);

            // Check that statement exist in RyaDAO
            final List<RyaStatement> results = ExampleUtils.queryRyaStatement(ryaDao, ryaStatement);
            if (results.isEmpty()) {
                System.out.println("No query results return from Rya for example: " + this.getClass().getSimpleName());
            }

            ryaDao.dropAndDestroy();
        }
    }

    public void usageWithOntModel(final String file) throws Exception {
        System.out.println("Results with OntModel");
        System.out.println("---------------------");
        System.out.println();

        // create an empty ontology model using Pellet spec
        final OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );

        // read the file
        model.read( file );

        // print validation report
        final ValidityReport report = model.validate();
        printIterator( report.getReports(), "Validation Results" );

        // print superclasses using the utility function
        final OntClass c = model.getOntClass( NS + "MaleStudentWith3Daughters" );
        printIterator(c.listSuperClasses(), "All super classes of " + c.getLocalName());
        // OntClass provides function to print *only* the direct subclasses
        printIterator(c.listSuperClasses(true), "Direct superclasses of " + c.getLocalName());

        System.out.println();

        if (conn != null) {
            System.out.println("Adding data to Rya repository...");
            // Dump model into Rya repository and check if we can query it.
            final MongoDBRyaDAO ryaDao = (MongoDBRyaDAO) ExampleUtils.getRyaDaoAndPopulateWithModel(conn, model);
            final RyaStatement ryaStatement = createRyaStatement(NS + "MaleStudentWith3Daughters", null, null);

            // Check that statement exist in RyaDAO
            final List<RyaStatement> results = ExampleUtils.queryRyaStatement(ryaDao, ryaStatement);
            if (results.isEmpty()) {
                System.out.println("No query results return from Rya for example: " + this.getClass().getSimpleName());
            }
        }
    }

    public static void printIterator(final Iterator<?> i, final String header) {
        System.out.println(header);
        for(int c = 0; c < header.length(); c++) {
            System.out.print("=");
        }
        System.out.println();

        if(i.hasNext()) {
            while (i.hasNext()) {
                System.out.println( i.next() );
            }
        } else {
            System.out.println("<EMPTY>");
        }

        System.out.println();
    }
}
