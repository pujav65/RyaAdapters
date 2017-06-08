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

import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

import org.apache.rya.api.domain.RyaStatement;
import org.apache.rya.api.persist.RyaDAO;
import org.apache.rya.jena.example.pellet.util.ExampleUtils;
import org.mindswap.pellet.PelletOptions;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.clarkparsia.owlapi.explanation.PelletExplanation;
import com.clarkparsia.owlapi.explanation.io.manchester.ManchesterSyntaxExplanationRenderer;
import com.clarkparsia.owlapiv3.OWL;
import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

/**
 * <p>
 * Title: ExplanationExample
 * </p>
 * <p>
 * Description: This program shows how to use Pellet's explanation service
 * </p>
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 */
public class ExplanationExample implements PelletExampleRunner {
    private static final String FILE = "file:src/main/resources/data/people+pets.owl";
    private static final String NS = "http://cohse.semanticweb.org/ontologies/people#";

    private final SailRepositoryConnection conn;

    /**
     * Creates a new instance of {@link ExplanationExample}.
     * @param conn the {@link SailRepositoryConnection}.
     */
    public ExplanationExample(final SailRepositoryConnection conn) {
        this.conn = conn;
    }

    @Override
    public void run() throws Exception {
        run(FILE);
    }

    public void run(final String file) throws Exception {
        PelletExplanation.setup();

        // The renderer is used to pretty print explanation
        final ManchesterSyntaxExplanationRenderer renderer = new ManchesterSyntaxExplanationRenderer();
        // The writer used for the explanation rendered
        final PrintWriter out = new PrintWriter( System.out );
        renderer.startRendering( out );

        // Create an OWLAPI manager that allows to load an ontology file and
        // create OWLEntities
        final OWLOntologyManager manager = OWL.manager;
        final OWLOntology ontology = manager.loadOntology( IRI.create( file ) );

        // Create the reasoner and load the ontology
        final PelletReasoner reasoner = PelletReasonerFactory.getInstance().createReasoner( ontology );

        // Create an explanation generator
        final PelletExplanation expGen = new PelletExplanation( reasoner );

        // Create some concepts
        final OWLClass madCow = OWL.Class( NS + "mad+cow" );
        final OWLClass animalLover = OWL.Class( NS + "animal+lover" );
        final OWLClass petOwner = OWL.Class( NS + "pet+owner" );

        // Explain why mad cow is an unsatisfiable concept
        Set<Set<OWLAxiom>> exp = expGen.getUnsatisfiableExplanations( madCow );
        out.println( "Why is " + madCow + " concept unsatisfiable?" );
        renderer.render( exp );

        // Now explain why animal lover is a sub class of pet owner
        exp = expGen.getSubClassExplanations( animalLover, petOwner );
        out.println( "Why is " + animalLover + " subclass of " + petOwner + "?" );
        renderer.render( exp );

        renderer.endRendering();

        if (conn != null) {
            System.out.println("Adding data to Rya repository...");
            // Dump ontology into Rya repository and check if we can query it.
            final RyaDAO<?> ryaDao = ExampleUtils.getRyaDaoAndPopulateWithOntology(conn, ontology);
            final RyaStatement ryaStatement = createRyaStatement(NS + "animal+lover", null, null);

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
        // PelletExplanation.setup() had set this to true. Set it back.
        PelletOptions.USE_TRACING = false;
    }

    public static void main(final String[] args) throws Exception {
        final ExplanationExample app = new ExplanationExample(null);
        app.run();
    }
}