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
import org.mindswap.pellet.utils.Timer;
import org.mindswap.pellet.utils.Timers;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.RemoveAxiom;

import com.clarkparsia.modularity.IncrementalClassifier;
import com.clarkparsia.owlapiv3.OWL;

/**
 * <p>
 * Title: IncrementalClassifierExample
 * </p>
 * <p>
 * Description: This programs shows the usage and performance of the incremental
 * classifier
 * </p>
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 */
public class IncrementalClassifierExample implements PelletExampleRunner {
    // The ontology we use for classification
    private static final String FILE = "file:src/main/resources/data/simple-galen.owl";
    // Don't modify this
    private static final String NS = "http://www.co-ode.org/ontologies/galen#";

    private final SailRepositoryConnection conn;

    /**
     * Creates a new instance of {@link IncrementalClassifierExample}.
     * @param conn the {@link SailRepositoryConnection}.
     */
    public IncrementalClassifierExample(final SailRepositoryConnection conn) {
        this.conn = conn;
    }

    @Override
    public void run() throws Exception {
        run(FILE);
    }

    public void run(final String file) throws Exception {
        // Load the ontology file into an OWL ontology object
        final OWLOntology ontology = OWL.manager.loadOntology( IRI.create( file ) );

        // Get some entities
        final OWLClass headache = OWL.Class( NS + "Headache" );
        final OWLClass pain = OWL.Class( NS + "Pain" );

        // Get an instance of the incremental classifier
        final IncrementalClassifier classifier = new IncrementalClassifier( ontology );

        // We need some timing to show the performance of the classification
        final Timers timers = new Timers();

        // We classify the ontology and use a specific timer to keep track of
        // the time required for the classification
        Timer t = timers.createTimer( "First classification" );
        t.start();
        classifier.classify();
        t.stop();

        System.out.println( "\nClassification time: " + t.getTotal() + "ms");
        System.out.println( "Subclasses of " + pain + ": " + classifier.getSubClasses( pain, true ).getFlattened() + "\n");

        // Now create a new OWL axiom, subClassOf(Headache, Pain)
        final OWLAxiom axiom = OWL.subClassOf( headache, pain );

        // Add the axiom to the ontology, which creates a change event
        OWL.manager.applyChange( new AddAxiom( ontology, axiom ) );

        // Now we create a second timer to keep track of the performance of the
        // second classification
        t = timers.createTimer( "Second classification" );
        t.start();
        classifier.classify();
        t.stop();

        System.out.println( "\nClassification time: " + t.getTotal() + "ms");
        System.out.println( "Subclasses of " + pain + ": " + classifier.getSubClasses( pain, true ).getFlattened() + "\n");

        // Remove the axiom from the ontology, which creates a change event
        OWL.manager.applyChange( new RemoveAxiom( ontology, axiom ) );

        // Now we create a third timer to keep track of the performance of the
        // third classification
        timers.startTimer( "Third classification" );
        classifier.classify();
        timers.stopTimer( "Third classification" );

        System.out.println( "\nClassification time: " + t.getTotal() + "ms");
        System.out.println( "Subclasses of " + pain + ": " + classifier.getSubClasses( pain, true ).getFlattened() + "\n");

        // Finally, print the timing. As you can see, the second classification
        // takes significantly less time, which is the characteristic of the
        // incremental classifier.
        System.out.println( "Timers summary" );
        for( final Timer timer : timers.getTimers() ) {
            if( !timer.isStarted() ) {
                System.out.println( timer.getName() + ": " + timer.getTotal() + "ms" );
            }
        }

        if (conn != null) {
            System.out.println("Adding data to Rya repository...");
            // Dump ontology into Rya repository and check if we can query it.
            final RyaDAO<?> ryaDao = ExampleUtils.getRyaDaoAndPopulateWithOntology(conn, ontology);
            final RyaStatement ryaStatement = createRyaStatement(NS + "Pain", null, null);

            // Check that statement exist in RyaDAO
            final List<RyaStatement> results = ExampleUtils.queryRyaStatement(ryaDao, ryaStatement);
            if (results.isEmpty()) {
                System.out.println("No query results return from Rya for example: " + this.getClass().getSimpleName());
            }
        }
    }

    public static void main(final String[] args) throws Exception {
        final IncrementalClassifierExample app = new IncrementalClassifierExample(null);
        app.run();
    }
}
