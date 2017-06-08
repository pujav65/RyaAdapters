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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.rya.api.domain.RyaStatement;
import org.apache.rya.jena.example.pellet.util.ExampleUtils;
import org.apache.rya.mongodb.MongoDBRyaDAO;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.clarkparsia.modularity.ModularityUtils;
import com.clarkparsia.owlapiv3.OWL;

import uk.ac.manchester.cs.owlapi.modularity.ModuleType;

/**
 * <p>
 * Title: ModularityExample
 * </p>
 * <p>
 * Description: This program shows the usage of Pellet's module extraction
 * service
 * </p>
 * <p>
 * Copyright: Copyright (c) 2008
 * </p>
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 */
public class ModularityExample implements PelletExampleRunner {
    private static final String FILE = "file:src/main/resources/data/simple-galen.owl";
    private static final String NS = "http://www.co-ode.org/ontologies/galen#";

    private final SailRepositoryConnection conn;

    /**
     * Creates a new instance of {@link ModularityExample}.
     * @param conn the {@link SailRepositoryConnection}.
     */
    public ModularityExample(final SailRepositoryConnection conn) {
        this.conn = conn;
    }

    @Override
    public void run() throws Exception {
        run(FILE);
    }

    public void run(final String file) throws Exception {
        // Create an OWLAPI manager that allows to load an ontology
        final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        // Load the ontology file into an OWL ontology object
        final OWLOntology ontology = manager.loadOntology( IRI.create( file ) );

        // Get some figures about the ontology and print them
        System.out.println( "The ontology contains "
            + ontology.getLogicalAxiomCount() + " axioms, "
            + ontology.getClassesInSignature().size() + " classes, and "
            + ontology.getObjectPropertiesInSignature().size() + " properties" );

        // Create the signature of the module with are interested to extract
        final Set<OWLEntity> signature = new HashSet<OWLEntity>();
        signature.add( OWL.Class( NS + "Heart" ) );
        signature.add( OWL.Class( NS + "Liver" ) );
        signature.add( OWL.Class( NS + "BloodPressure" ) );

        // Select a module type. Modules contain axioms related to the signature
        // elements that describe how they relate to each other. There are four
        // module types supported with the following very rough explanations:
        // * lower (top) module
        //   contains subclasses of the signature elements
        // * upper (bot) module
        //   contains superclasses of the signature elements
        // * upper-of-lower (bot_of_top) module
        //   extract the upper module from the lower module
        // * lower-of-upper (top_of_bot) module -
        //   extract the lower module from the upper module
        //
        // The module types are closely related to the locality class used. Lower
        // module is extracted with top locality and thus also called top module.
        //
        // Upper-of-lower and lower-of-upper modules tend to be smaller (compared
        // to upper and lower modules) and we'll extract upper-of-lower module in
        // this example
        final ModuleType moduleType = ModuleType.TOP_OF_BOT;

        // Extract the module axioms for the specified signature
        final Set<OWLAxiom> moduleAxioms =
            ModularityUtils.extractModule( ontology, signature, moduleType );
        // Create an ontology for the module axioms
        final OWLOntology moduleOnt = manager.createOntology( moduleAxioms );

        // Get some figures about the extracted module and print them
        System.out.println( "The module contains "
            + moduleOnt.getLogicalAxiomCount() + " axioms, "
            + moduleOnt.getClassesInSignature().size() + " classes, and "
            + moduleOnt.getObjectPropertiesInSignature().size() + " properties" );

        if (conn != null) {
            System.out.println("Adding data to Rya repository...");
            // Dump ontology into Rya repository and check if we can query it.
            final MongoDBRyaDAO ryaDao = (MongoDBRyaDAO) ExampleUtils.getRyaDaoAndPopulateWithOntology(conn, ontology);
            final RyaStatement ryaStatement = createRyaStatement(NS + "Heart", null, null);

            // Check that statement exist in RyaDAO
            final List<RyaStatement> results = ExampleUtils.queryRyaStatement(ryaDao, ryaStatement);
            if (results.isEmpty()) {
                System.out.println("No query results return from Rya for example: " + this.getClass().getSimpleName());
            }
        }
    }

    public static void main(final String[] args) throws Exception {
        final ModularityExample app = new ModularityExample(null);
        app.run();
    }
}
