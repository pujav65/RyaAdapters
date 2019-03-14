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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.rya.api.domain.RyaStatement;
import org.apache.rya.jena.example.pellet.util.ExampleUtils;
import org.apache.rya.mongodb.MongoDBRyaDAO;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.clarkparsia.modularity.IncrementalClassifier;
import com.clarkparsia.modularity.io.IncrementalClassifierPersistence;
import com.clarkparsia.owlapiv3.OWL;

/**
 * <p>
 * Title: PersistenceExample
 * </p>
 * <p>
 * Description: This program shows the usage of the persistence feature in IncrementalClassifier.
 * </p>
 * <p>
 * Copyright: Copyright (c) 2010
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 */
public class PersistenceExample implements PelletExampleRunner {
    // The ontology we use for classification
    private static final String FILE = "file:src/main/resources/data/simple-galen.owl";

    // The zip archive that will be created to store the internal data of the incremental classifier
    private static final String PERSISTENCE_FILE = "incrementalClassifierData.zip";

    // Don't modify this
    private static final String NS = "http://www.co-ode.org/ontologies/galen#";

    private final SailRepositoryConnection conn;

    /**
     * Creates a new instance of {@link PersistenceExample}.
     * @param conn the {@link SailRepositoryConnection}.
     */
    public PersistenceExample(final SailRepositoryConnection conn) {
        this.conn = conn;
    }

    @Override
    public void run() throws Exception {
        run(FILE, PERSISTENCE_FILE);
    }

    public void run(final String file, final String persistenceFile) throws Exception {
        // Load the ontology file into an OWL ontology object
        final IRI iri = IRI.create( file );
        final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        final OWLOntology ontology = manager.loadOntology(iri);

        // Get an instance of the incremental classifier
        final IncrementalClassifier classifier = new IncrementalClassifier( ontology );

        // trigger classification
        classifier.classify();

        // persist the current state of the classifier to a file
        try {
            System.out.print( "Saving the state of the classifier to the file ... " );
            System.out.flush();

            // open the stream to a file
            final FileOutputStream outputStream = new FileOutputStream( persistenceFile );

            // write the contents to the stream
            IncrementalClassifierPersistence.save( classifier, outputStream );

            // close stream
            outputStream.close();

            System.out.println( "done." );
        } catch( final IOException e ) {
            System.out.println( "I/O Error occurred while saving the current state of the incremental classifier: " + e );
            System.exit(1);
        }


        // The following code introduces a few changes to the ontology, while the internal state of the classifier is stored in a file.
        // Later, the classifier read back from the file will automatically notice the changes, and incrementally apply them

        final OWLClass headache = OWL.Class( NS + "Headache" );
        final OWLClass pain = OWL.Class( NS + "Pain" );

        // Now create a new OWL axiom, subClassOf(Headache, Pain)
        final OWLAxiom axiom = OWL.subClassOf( headache, pain );

        // Add the axiom to the ontology
        // The copy of the classifier in memory, will receive the notification about this change.
        // However, the state of the classifier saved to the file will become out-of-sync at this point
        manager.applyChange( new AddAxiom( ontology, axiom ) );


        // Now let's restore the classifier from the saved file
        IncrementalClassifier restoredClassifier = null;

        try {
            System.out.print( "Reading the state of the classifier back from the file ... ");
            System.out.flush();

            // open the previously saved file
            final FileInputStream inputStream = new FileInputStream( persistenceFile );

            // restore the classifier from the file

            // it is important to provide the ontology here, if we want the classifier to notice the changes that occurred while the
            // state was stored in the file, and incrementally update the classifier's state
            // (IncrementalClassifierPersistence has another "load" method without ontology parameter, which can be used
            // for cases when there is no ontology to compare).
            restoredClassifier = IncrementalClassifierPersistence.load( inputStream, ontology );

            // close stream
            inputStream.close();
            System.out.println( "done." );
        } catch( final IOException e ) {
            System.out.println( "I/O Error occurred while reading the current state of the incremental classifier: " + e );
            System.exit(1);
        }

        // Now query both of the classifiers for subclasses of "Pain" class. Both of the classifiers will incrementally update their state, and should print
        // the same information

        System.out.println( "[Original classifier] Subclasses of " + pain + ": " + classifier.getSubClasses( pain, true ).getFlattened() + "\n");
        System.out.println( "[Restored classifier] Subclasses of " + pain + ": " + restoredClassifier.getSubClasses( pain, true ).getFlattened() + "\n");

        // clean up by removing the file containing the persisted state
        final File fileToDelete = new File( persistenceFile );
        fileToDelete.delete();

        if (conn != null) {
            System.out.println("Adding data to Rya repository...");
            // Dump ontology into Rya repository and check if we can query it.
            final MongoDBRyaDAO ryaDao = (MongoDBRyaDAO) ExampleUtils.getRyaDaoAndPopulateWithOntology(conn, ontology);
            final RyaStatement ryaStatement = createRyaStatement(NS + "Pain", null, null);

            // Check that statement exist in RyaDAO
            final List<RyaStatement> results = ExampleUtils.queryRyaStatement(ryaDao, ryaStatement);
            if (results.isEmpty()) {
                System.out.println("No query results return from Rya for example: " + this.getClass().getSimpleName());
            }
        }
    }

    public static void main(final String[] args) throws Exception {
        final PersistenceExample app = new PersistenceExample(null);
        app.run();
    }
}
