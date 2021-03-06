/*
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
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */
package org.netbeans.lib.cvsclient.command.annotate;

import java.io.File;
import java.io.IOException;

import org.netbeans.lib.cvsclient.command.BasicCommand;
import org.netbeans.lib.cvsclient.command.Builder;
import org.netbeans.lib.cvsclient.event.EventManager;
import org.netbeans.lib.cvsclient.event.FileInfoEvent;

/**
 * Handles the building of a annotate information object and the firing of
 * events when complete objects are built.
 * 
 * @author Milos Kleint
 */
public class AnnotateBuilder implements Builder {
    private static final String ANNOTATING = "Annotations for "; // NOI18N
    private static final String STARS = "***************"; // NOI18N

    /**
     * The Annotate object that is currently being built.
     */
    private AnnotateInformation annotateInformation;

    /**
     * The event manager to use.
     */
    private final EventManager eventManager;

    private final String localPath;
    private final File tempDir;

    public AnnotateBuilder(final EventManager eventManager, final BasicCommand annotateCommand) {
        this.eventManager = eventManager;
        localPath = annotateCommand.getLocalDirectory();
        tempDir = annotateCommand.getGlobalOptions().getTempDir();
    }

    public void outputDone() {
        if (annotateInformation == null) {
            return;
        }

        try {
            annotateInformation.closeTempFile();
        } catch (final IOException exc) {
            // ignore
        }
        eventManager.fireCVSEvent(new FileInfoEvent(this, annotateInformation));
        annotateInformation = null;
    }

    public void parseLine(final String line, final boolean isErrorMessage) {
        if (isErrorMessage && line.startsWith(ANNOTATING)) {
            outputDone();
            annotateInformation = new AnnotateInformation(tempDir);
            annotateInformation.setFile(createFile(line.substring(ANNOTATING.length())));
            return;
        }

        if (isErrorMessage && line.startsWith(STARS)) {
            // skip
            return;
        }

        if (!isErrorMessage) {
            processLines(line);
        }
    }

    private File createFile(final String fileName) {
        return new File(localPath, fileName);
    }

    public void parseEnhancedMessage(final String key, final Object value) {
    }

    private void processLines(final String line) {
        if (annotateInformation != null) {
            try {
                annotateInformation.addToTempFile(line);
            } catch (final IOException exc) {
                // just ignore, should not happen.. if it does the worst thing
                // that happens is a annotate info without data..
            }
        }
        /*
         * AnnotateLine annLine = processLine(line); if (annotateInformation !=
         * null && annLine != null) { annLine.setLineNum(lineNum);
         * annotateInformation.addLine(annLine); lineNum++; }
         */
    }

    public static AnnotateLine processLine(final String line) {
        final int indexOpeningBracket = line.indexOf('(');
        final int indexClosingBracket = line.indexOf(')');
        AnnotateLine annLine = null;
        if ((indexOpeningBracket > 0) && (indexClosingBracket > indexOpeningBracket)) {
            final String revision = line.substring(0, indexOpeningBracket).trim();
            final String userDate = line.substring(indexOpeningBracket + 1, indexClosingBracket);
            final String contents = line.substring(indexClosingBracket + 3);
            final int lastSpace = userDate.lastIndexOf(' ');
            String user = userDate;
            String date = userDate;
            if (lastSpace > 0) {
                user = userDate.substring(0, lastSpace).trim();
                date = userDate.substring(lastSpace).trim();
            }
            annLine = new AnnotateLine();
            annLine.setContent(contents);
            annLine.setAuthor(user);
            annLine.setDateString(date);
            annLine.setRevision(revision);
        }
        return annLine;
    }
}
