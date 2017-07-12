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
package org.apache.rya.adapter.rest.reasoner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.apache.rya.adapter.rest.reasoner.types.ReasonerResult;
import org.apache.rya.api.RdfCloudTripleStoreConfiguration;
import org.apache.rya.api.domain.RyaStatement;
import org.apache.rya.api.domain.RyaType;
import org.apache.rya.api.domain.RyaURI;
import org.apache.rya.api.persist.RyaDAO;
import org.apache.rya.rdftriplestore.RdfCloudTripleStore;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

/**
 * Service that uploads a reasoner file to the server in order to performing
 * reasoning on it.
 */
@Path("/ReasonerService")
@Component
public class ReasonerService {
    private final static Logger log = Logger.getLogger(ReasonerService.class);

    private static final String CONFIG_PRINT_STATEMENTS = "rya.adapter.web.print.statements";
    private static final String SERVER_UPLOAD_LOCATION_FOLDER = "/uploaded_rya_ontologies/";
    private static final String CRNL = "\r\n";

    private final ReasonerDao reasonerDao = new ReasonerDao();

    @Autowired
    private SailRepository repository;

    /**
     * Upload a file to the server from the client's form inputs.
     * @param uploadedInputStream the {@link InputStream} of the file to upload.
     * @param fileDetail {@link FormDataContentDisposition} containing details
     * about the file.
     * @param hiddenFilename the path of the file to upload.
     * @param reasonerTypeName the name of the {@link ReasonerType} to use.
     * @return the {@link Response}.
     */
    @POST
    @Path("/uploadFile")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("text/html")
    public Response uploadFile(
            @FormDataParam("file") final InputStream uploadedInputStream,
            @FormDataParam("file") final FormDataContentDisposition fileDetail,
            @FormDataParam("hiddenFilename") final String hiddenFilename,
            @FormDataParam("reasonerType") final String reasonerTypeName) {
        // TODO: Use fileDetail when IE supports it.  Until then use the workaround with the hidden filename input
        //final String uploadedFileLocation = SERVER_UPLOAD_LOCATION_FOLDER + fileDetail.getFileName();
        final String uploadedFileLocation = SERVER_UPLOAD_LOCATION_FOLDER + FilenameUtils.getName(hiddenFilename);

        SailRepositoryConnection conn = null;
        try {
            conn = repository.getConnection();
        } catch (final Exception e) {
            final String errorMessage = "Failed to connect to repository";
            log.error(errorMessage, e);
            return Response.status(-1).entity(errorMessage).build();
        }

        final RdfCloudTripleStore rdfCloudTripleStore = ((RdfCloudTripleStore)repository.getSail());
        final RyaDAO<?> ryaDao = rdfCloudTripleStore.getRyaDAO();
        final RdfCloudTripleStoreConfiguration conf = ryaDao.getConf();

        final File file = new File(uploadedFileLocation);
        if (file.exists()) {
            log.info(uploadedFileLocation + " already exists");
        } else {
            // Save it
            try {
                writeToFile(uploadedInputStream, uploadedFileLocation);
            } catch (final Exception e) {
                final String errorMessage = "Failed to write ontology file on the server";
                log.error(errorMessage, e);
                return Response.status(-1).entity(errorMessage).build();
            }
        }

        final ReasonerType reasonerType = ReasonerType.fromName(reasonerTypeName);
        ReasonerResult reasonerResult;
        try {
            reasonerResult = reasonerDao.startReasoner(conn, uploadedFileLocation, reasonerType);
        } catch (final Exception e) {
            final String errorMessage = "Error encountered while running reasoner on the ontology file on the server";
            log.error(errorMessage, e);
            return Response.status(-1).entity(errorMessage).build();
        }

        final boolean shouldPrintStatements = conf.getBoolean(CONFIG_PRINT_STATEMENTS, false);
        final Object resultPage = createResponsePage(uploadedFileLocation, reasonerResult.getRyaStatements(), shouldPrintStatements);

        final Response response = Response.status(200).entity(resultPage).build();
        return response;
    }

    private static Object createResponsePage(final String uploadedFileLocation, final List<RyaStatement> ryaStatements, final boolean shouldPrintStatements) {
        final String outputLocation = "<b>File uploaded to:</b> " + uploadedFileLocation;
        log.info(outputLocation.replace("<b>", "").replace("</b>", ""));

        final int count = ryaStatements.size();
        final String resultsCount = "<b>Statements returned:</b> " + count;
        log.info(resultsCount.replace("<b>", "").replace("</b>", ""));

        final StringBuilder sb = new StringBuilder();
        sb.append("<html>" + CRNL);
        sb.append("<body>" + CRNL);
        sb.append("<h1>Reasoner Results:</h1>" + CRNL);
        sb.append(outputLocation + CRNL);
        sb.append("<br><br>" + CRNL);
        sb.append(resultsCount + CRNL);
        if (shouldPrintStatements) {
            // Create Rya Statements table
            sb.append("<br><br>" + CRNL);
            sb.append("<table border=\"1\">" + CRNL);
            sb.append("<caption><h3>Rya Statements</h3></caption>" + CRNL);
            sb.append("<tr>");
            sb.append("<th>");
            sb.append("Subject");
            sb.append("</th>");
            sb.append("<th>");
            sb.append("Predicate");
            sb.append("</th>");
            sb.append("<th>");
            sb.append("Object");
            sb.append("</th>");
            sb.append("</tr>" + CRNL);
            for (final RyaStatement ryaStatement : ryaStatements) {
                final RyaURI subject = ryaStatement.getSubject();
                final RyaURI predicate = ryaStatement.getPredicate();
                final RyaType object = ryaStatement.getObject();
                sb.append("<tr>");
                sb.append("<td>");
                if (subject != null) {
                    if (subject.getData() != null) {
                        sb.append(subject.getData());
                    }
                }
                sb.append("</td>");
                sb.append("<td>");
                if (predicate != null) {
                    if (predicate.getData() != null) {
                        sb.append(predicate.getData());
                    }
                }
                sb.append("</td>");
                sb.append("<td>");
                if (object != null) {
                    if (object.getData() != null) {
                        sb.append(object.getData());
                    }
                }
                sb.append("</td>");
                sb.append("</tr>" + CRNL);
            }
            sb.append("</table>" + CRNL);
        }
        sb.append("</body>" + CRNL);
        sb.append("</html>" + CRNL);

        return sb.toString();
    }

    /**
     * Save uploaded file to new location.
     * @param uploadedInputStream the {@link InputStream} to read from.
     * @param uploadedFileLocation the file location path to write to.
     * @throws Exception
     */
    private static void writeToFile(final InputStream uploadedInputStream, final String uploadedFileLocation) throws Exception {
        int read = 0;
        final byte[] bytes = new byte[1024];
        final File file = new File(uploadedFileLocation);
        file.getParentFile().mkdirs();
        try (final OutputStream out = new FileOutputStream(file)) {
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            out.flush();
        } finally {
            if (uploadedInputStream != null) {
                uploadedInputStream.close();
            }
        }
    }
}