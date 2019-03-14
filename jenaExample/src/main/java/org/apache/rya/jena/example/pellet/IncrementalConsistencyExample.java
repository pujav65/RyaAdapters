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

import java.util.List;

import org.apache.rya.api.domain.RyaStatement;
import org.apache.rya.api.persist.RyaDAO;
import org.apache.rya.jena.example.pellet.util.ExampleUtils;
import org.mindswap.pellet.KnowledgeBase;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.jena.PelletInfGraph;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.mindswap.pellet.utils.ATermUtils;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

import aterm.ATermAppl;

/**
 * An example program that incrementally checks consistency through additions to
 * the ABox. The example demonstrates the necessary flags that need to be set,
 * which enable the incremental consistency checking service. Currently the
 * incremental consistency checking service can  be used through
 * the Pellet, Jena and OWL APIs. The example loads an ontology, makes ABox
 * changes and incrementally performs consistency checks.
 */
public class IncrementalConsistencyExample implements PelletExampleRunner {
    // namespaces that will be used
    private static final String FOAF = "http://xmlns.com/foaf/0.1/";

    private static final String MINDSWAP = "http://www.mindswap.org/2003/owl/mindswap#";

    private static final String MINDSWAPPERS = "http://www.mindswap.org/2004/owl/mindswappers#";

    private static final String FILE = "file:src/main/resources/data/discovery_short.owl";

    private final SailRepositoryConnection conn;

    /**
     * Creates a new instance of {@link IncrementalConsistencyExample}.
     * @param conn the {@link SailRepositoryConnection}.
     */
    public IncrementalConsistencyExample(final SailRepositoryConnection conn) {
        this.conn = conn;
    }

    @Override
    public void run() throws Exception {
        run(FILE);
    }

    public void run(final String file) throws Exception {
        // Set flags for incremental consistency
        PelletOptions.USE_COMPLETION_QUEUE = true;
        PelletOptions.USE_INCREMENTAL_CONSISTENCY = true;
        PelletOptions.USE_SMART_RESTORE = false;

        runWithPelletAPI(file);

        runWithOWLAPI(file);

        runWithJenaAPIAndPelletInfGraph(file);

        runWithJenaAPIAndOntModel(file);
    }

    @Override
    public void cleanUp() throws Exception {
        PelletExampleRunner.super.cleanUp();
        PelletOptions.USE_COMPLETION_QUEUE = false;
        PelletOptions.USE_INCREMENTAL_CONSISTENCY = false;
        PelletOptions.USE_SMART_RESTORE = true;
    }

    public static void main(final String[] args) throws Exception {
        final IncrementalConsistencyExample app = new IncrementalConsistencyExample(null);
        app.run();
    }

    public void runWithPelletAPI(final String file) throws Exception {
        System.out.println( "\nResults after applying changes through Pellet API" );
        System.out.println( "-------------------------------------------------" );

        // read the ontology with its imports
        final OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        model.read( file );

        // load the model to the reasoner
        model.prepare();

        // Get the KnolwedgeBase object
        final KnowledgeBase kb = ((PelletInfGraph) model.getGraph()).getKB();

        // perform initial consistency check
        long s = System.currentTimeMillis();
        boolean consistent = kb.isConsistent();
        long e = System.currentTimeMillis();
        System.out.println( "Consistent? " + consistent + " (" + (e - s) + "ms)" );

        // perform ABox addition which results in a consistent KB
        final ATermAppl concept = ATermUtils.makeTermAppl( MINDSWAP + "GraduateStudent" );
        ATermAppl individual = ATermUtils.makeTermAppl( MINDSWAPPERS + "JohnDoe" );
        kb.addIndividual( individual );
        kb.addType( individual, concept );

        // perform incremental consistency check
        s = System.currentTimeMillis();
        consistent = kb.isConsistent();
        e = System.currentTimeMillis();
        System.out.println( "Consistent? " + consistent + " (" + (e - s) + "ms)" );

        // perform ABox addition which results in an inconsistent KB
        final ATermAppl role = ATermUtils.makeTermAppl( FOAF + "mbox" );
        individual = ATermUtils.makeTermAppl( MINDSWAPPERS + "Christian.Halaschek" );
        final ATermAppl mbox = ATermUtils.makeTermAppl( "mailto:kolovski@cs.umd.edu" );
        kb.addPropertyValue( role, individual, mbox );

        // perform incremental consistency check
        s = System.currentTimeMillis();
        consistent = kb.isConsistent();
        e = System.currentTimeMillis();
        System.out.println( "Consistent? " + consistent + " (" + (e - s) + "ms)" );

        if (conn != null) {
            System.out.println("Adding data to Rya repository...");
            // Dump model into Rya repository and check if we can query it.
            final RyaDAO<?> ryaDao = ExampleUtils.getRyaDaoAndPopulateWithModel(conn, model);
            final RyaStatement ryaStatement = createRyaStatement(MINDSWAPPERS + "JohnDoe", null, null);

            // Check that statement exist in RyaDAO
            final List<RyaStatement> results = ExampleUtils.queryRyaStatement(ryaDao, ryaStatement);
            if (results.isEmpty()) {
                System.out.println("No query results return from Rya for example: " + this.getClass().getSimpleName());
            }
        }
    }

    public void runWithOWLAPI(final String file) throws Exception {
        System.out.println( "\nResults after applying changes through OWL API" );
        System.out.println( "----------------------------------------------" );

        // read the ontology
        final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        final OWLDataFactory factory = manager.getOWLDataFactory();
        final OWLOntology ontology = manager.loadOntology( IRI.create( file ) );

        // we want a non-buffering reasoner here (a buffering reasoner would not process any additions, until manually refreshed)
        final PelletReasoner reasoner = com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory.getInstance().createNonBufferingReasoner( ontology );
        manager.addOntologyChangeListener( reasoner );

        // perform initial consistency check
        long s = System.currentTimeMillis();
        boolean consistent = reasoner.isConsistent();
        long e = System.currentTimeMillis();
        System.out.println( "Consistent? " + consistent + " (" + (e - s) + "ms)" );

        // perform ABox addition which results in a consistent KB
        final OWLClass concept = factory.getOWLClass( IRI.create( MINDSWAP + "GraduateStudent" ) );
        OWLNamedIndividual individual = factory
                .getOWLNamedIndividual( IRI.create( MINDSWAPPERS + "JohnDoe" ) );
        manager.applyChange( new AddAxiom( ontology, factory.getOWLClassAssertionAxiom( concept, individual ) ) );

        // perform incremental consistency check
        s = System.currentTimeMillis();
        consistent = reasoner.isConsistent();
        e = System.currentTimeMillis();
        System.out.println( "Consistent? " + consistent + " (" + (e - s) + "ms)" );

        // perform ABox addition which results in an inconsistent KB
        final OWLObjectProperty role = factory.getOWLObjectProperty( IRI.create( FOAF + "mbox" ) );
        individual = factory.getOWLNamedIndividual( IRI.create( MINDSWAPPERS + "Christian.Halaschek" ) );
        final OWLNamedIndividual mbox = factory.getOWLNamedIndividual( IRI.create( "mailto:kolovski@cs.umd.edu" ) );
        manager.applyChange( new AddAxiom( ontology, factory.getOWLObjectPropertyAssertionAxiom(
                role, individual, mbox ) ) );

        // perform incremental consistency check
        s = System.currentTimeMillis();
        consistent = reasoner.isConsistent();
        e = System.currentTimeMillis();
        System.out.println( "Consistent? " + consistent + " (" + (e - s) + "ms)" );

        if (conn != null) {
            System.out.println("Adding data to Rya repository...");
            // Dump ontology into Rya repository and check if we can query it.
            final RyaDAO<?> ryaDao = ExampleUtils.getRyaDaoAndPopulateWithOntology(conn, ontology);
            final RyaStatement ryaStatement = createRyaStatement(MINDSWAPPERS + "JohnDoe", null, null);

            // Check that statement exist in RyaDAO
            final List<RyaStatement> results = ExampleUtils.queryRyaStatement(ryaDao, ryaStatement);
            if (results.isEmpty()) {
                System.out.println("No query results return from Rya for example: " + this.getClass().getSimpleName());
            }
        }
    }

    public void runWithJenaAPIAndPelletInfGraph(final String file) throws Exception {
        System.out.println( "\nResults after applying changes through Jena API using PelletInfGraph" );
        System.out.println( "-------------------------------------------------" );

        // read the ontology using model reader
        final OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        model.setStrictMode( false );
        model.read( file );

        //get the PelletInfGraph object
        final PelletInfGraph pelletJenaGraph = ( PelletInfGraph )model.getGraph();

        // perform initial consistency check
        long s = System.currentTimeMillis();
        boolean consistent = pelletJenaGraph.isConsistent();
        long e = System.currentTimeMillis();
        System.out.println( "Consistent? " + consistent + " (" + (e - s) + "ms)" );

        // perform ABox addition which results in a consistent KB
        final Resource concept = model.getResource( MINDSWAP + "GraduateStudent" );
        Individual individual = model.createIndividual( MINDSWAPPERS + "JohnDoe", concept );

        // perform incremental consistency check
        s = System.currentTimeMillis();
        consistent = pelletJenaGraph.isConsistent();
        e = System.currentTimeMillis();
        System.out.println( "Consistent? " + consistent + " (" + (e - s) + "ms)" );

        // perform ABox addition which results in an inconsistent KB
        final Property role = model.getProperty( FOAF + "mbox" );
        individual = model.getIndividual( MINDSWAPPERS + "Christian.Halaschek" );
        final RDFNode mbox = model.getIndividual( "mailto:kolovski@cs.umd.edu" );
        if (individual != null) {
            individual.addProperty( role, mbox );
        }

        // perform incremental consistency check
        s = System.currentTimeMillis();
        consistent = pelletJenaGraph.isConsistent();
        e = System.currentTimeMillis();
        System.out.println( "Consistent? " + consistent + " (" + (e - s) + "ms)" );

        if (conn != null) {
            System.out.println("Adding data to Rya repository...");
            // Dump model into Rya repository and check if we can query it.
            final RyaDAO<?> ryaDao = ExampleUtils.getRyaDaoAndPopulateWithModel(conn, model);
            final RyaStatement ryaStatement = createRyaStatement(MINDSWAPPERS + "JohnDoe", null, null);

            // Check that statement exist in RyaDAO
            final List<RyaStatement> results = ExampleUtils.queryRyaStatement(ryaDao, ryaStatement);
            if (results.isEmpty()) {
                System.out.println("No query results return from Rya for example: " + this.getClass().getSimpleName());
            }
        }
    }

    public void runWithJenaAPIAndOntModel(final String file) throws Exception {
        System.out.println( "\nResults after applying changes through Jena API using OntModel" );
        System.out.println( "-------------------------------------------------" );


        // read the ontology using model reader
        final OntModel model = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        model.setStrictMode( false );
        model.read( file );

        // perform initial consistency check
        long s = System.currentTimeMillis();
        model.prepare();
        long e = System.currentTimeMillis();

        //print time and validation report
        System.out.println( "Total time " + (e - s) + " ms)" );
        JenaReasoner.printIterator( model.validate().getReports(), "Validation Results" );

        // perform ABox addition which results in a consistent KB
        final Resource concept = model.getResource( MINDSWAP + "GraduateStudent" );
        Individual individual = model.createIndividual( MINDSWAPPERS + "JohnDoe", concept );

        // perform incremental consistency check
        s = System.currentTimeMillis();
        model.prepare();
        e = System.currentTimeMillis();

        //print time and validation report
        System.out.println( "Total time " + (e - s) + " ms)" );
        JenaReasoner.printIterator( model.validate().getReports(), "Validation Results" );

        // perform ABox addition which results in an inconsistent KB
        final Property role = model.getProperty( FOAF + "mbox" );
        individual = model.getIndividual( MINDSWAPPERS + "Christian.Halaschek" );
        final RDFNode mbox = model.getIndividual( "mailto:kolovski@cs.umd.edu" );
        if (individual != null) {
            individual.addProperty( role, mbox );
        }

        // perform incremental consistency check
        s = System.currentTimeMillis();
        model.prepare();
        e = System.currentTimeMillis();

        //print time and validation report
        System.out.println( "Total time " + (e - s) + " ms)" );
        JenaReasoner.printIterator( model.validate().getReports(), "Validation Results" );

        if (conn != null) {
            System.out.println("Adding data to Rya repository...");
            // Dump model into Rya repository and check if we can query it.
            final RyaDAO<?> ryaDao = ExampleUtils.getRyaDaoAndPopulateWithModel(conn, model);
            final RyaStatement ryaStatement = createRyaStatement(MINDSWAPPERS + "JohnDoe", null, null);

            // Check that statement exist in RyaDAO
            final List<RyaStatement> results = ExampleUtils.queryRyaStatement(ryaDao, ryaStatement);
            if (results.isEmpty()) {
                System.out.println("No query results return from Rya for example: " + this.getClass().getSimpleName());
            }
        }
    }
}
