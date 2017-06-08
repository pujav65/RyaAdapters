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
package org.apache.rya.jena.example.pellet.util;

import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.util.RDFInserter;
import org.openrdf.rio.RDFHandlerException;

/**
 * PODD is an OWL ontology database used for scientific project management
 *
 * Copyright (C) 2009-2013 The University Of Queensland
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */
import info.aduna.iteration.Iterations;

/**
 * An RDFInserter sub-class which only inserts statements that are not already present in another
 * given RDF graph.
 *
 * @author kutila
 */
public class DeduplicatingRDFInserter extends RDFInserter
{

    private URI contextToCompareWith = null;

    // TODO: Remove this and use super.con once it is made protected in the
    // parent class
    private RepositoryConnection conn = null;

    public DeduplicatingRDFInserter(final URI contextToCompareWith, final RepositoryConnection conn)
    {
        super(conn);
        this.contextToCompareWith = contextToCompareWith;
        this.conn = conn;
    }

    @Override
    public void handleStatement(final Statement st) throws RDFHandlerException
    {
        if(this.contextToCompareWith == null || this.conn == null)
        {
            super.handleStatement(st);
            return;
        }

        // check if statement exists in other context and ignore if so
        final Resource subj = st.getSubject();
        final URI pred = st.getPredicate();
        final Value obj = st.getObject();

        try
        {
            final List<Statement> duplicateStatements =
                    Iterations.asList(this.conn.getStatements(subj, pred, obj, false, this.contextToCompareWith));
            if(duplicateStatements == null || duplicateStatements.isEmpty())
            {
                super.handleStatement(st);
            }
            // else
            // {
            // System.out.println(" DUPLICATE statement. Skip. " +
            // contextToCompareWith.toString());
            // }
        }
        catch(final RepositoryException e)
        {
            throw new RDFHandlerException(e);
        }
    }
}