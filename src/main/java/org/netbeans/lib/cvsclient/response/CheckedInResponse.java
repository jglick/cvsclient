/*****************************************************************************
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is the CVS Client Library.
 * The Initial Developer of the Original Software is Robert Greig.
 * Portions created by Robert Greig are Copyright (C) 2000.
 * All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s): Robert Greig.
 *****************************************************************************/
package org.netbeans.lib.cvsclient.response;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import org.netbeans.lib.cvsclient.admin.Entry;
import org.netbeans.lib.cvsclient.util.LoggedDataInputStream;

/**
 * Indicates that a file has been successfully operated on, e.g. checked in,
 * added etc.
 * 
 * @author Robert Greig
 */
class CheckedInResponse implements Response {

    /**
     * 
     */
    private static final long serialVersionUID = -4489238799861869876L;
    /**
     * The date Formatter used to parse and format dates. Format is:
     * "EEE MMM dd HH:mm:ss yyyy"
     */
    private DateFormat dateFormatter;

    /**
     * Process the data for the response.
     * 
     * @param dis
     *            the data inputstream allowing the client to read the server's
     *            response. Note that the actual response name has already been
     *            read and the input stream is positioned just before the first
     *            argument, if any.
     */
    public void process(final LoggedDataInputStream dis, final ResponseServices services) throws ResponseException {
        try {
            final String localPath = dis.readLine();
            // System.err.println("Pathname is: " + localPath);
            final String repositoryPath = dis.readLine();
            // System.err.println("Repository path is: " + repositoryPath);
            final String entriesLine = dis.readLine();
            // System.err.println("New entries line is: " + entriesLine);

            final String absPath = services.convertPathname(localPath, repositoryPath);
            if (services.getGlobalOptions().isExcluded(new File(absPath))) {
                return;
            }

            // we set the date the file was last modified in the Entry line
            // so that we can easily determine whether the file has been
            // untouched
            final File theFile = new File(absPath);
            final Date date = new Date(theFile.lastModified());
            final Entry entry = new Entry(entriesLine);
            entry.setConflict(getDateFormatter().format(date));

            // for added and removed entries set the conflict to Dummy
            // timestamp.
            if (entry.isNewUserFile() || entry.isUserFileToBeRemoved()) {
                entry.setConflict(Entry.DUMMY_TIMESTAMP);
            }

            services.setEntry(theFile, entry);
        } catch (final IOException e) {
            throw new ResponseException((Exception) e.fillInStackTrace(), e.getLocalizedMessage());
        }
    }

    /**
     * Is this a terminal response, i.e. should reading of responses stop after
     * this response. This is true for responses such as OK or an error response
     */
    public boolean isTerminalResponse() {
        return false;
    }

    /**
     * Returns the DateFormatter instance that parses and formats date Strings.
     * The exact format matches the one in Entry.getLastModifiedDateFormatter()
     * method.
     * 
     */
    protected DateFormat getDateFormatter() {
        if (dateFormatter == null) {
            dateFormatter = Entry.getLastModifiedDateFormatter();
        }
        return dateFormatter;
    }

}
