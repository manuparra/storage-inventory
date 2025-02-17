/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2019.                            (c) 2019.
*  Government of Canada                 Gouvernement du Canada
*  National Research Council            Conseil national de recherches
*  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
*  All rights reserved                  Tous droits réservés
*
*  NRC disclaims any warranties,        Le CNRC dénie toute garantie
*  expressed, implied, or               énoncée, implicite ou légale,
*  statutory, of any kind with          de quelque nature que ce
*  respect to the software,             soit, concernant le logiciel,
*  including without limitation         y compris sans restriction
*  any warranty of merchantability      toute garantie de valeur
*  or fitness for a particular          marchande ou de pertinence
*  purpose. NRC shall not be            pour un usage particulier.
*  liable in any event for any          Le CNRC ne pourra en aucun cas
*  damages, whether direct or           être tenu responsable de tout
*  indirect, special or general,        dommage, direct ou indirect,
*  consequential or incidental,         particulier ou général,
*  arising from the use of the          accessoire ou fortuit, résultant
*  software.  Neither the name          de l'utilisation du logiciel. Ni
*  of the National Research             le nom du Conseil National de
*  Council of Canada nor the            Recherches du Canada ni les noms
*  names of its contributors may        de ses  participants ne peuvent
*  be used to endorse or promote        être utilisés pour approuver ou
*  products derived from this           promouvoir les produits dérivés
*  software without specific prior      de ce logiciel sans autorisation
*  written permission.                  préalable et particulière
*                                       par écrit.
*
*  This file is part of the             Ce fichier fait partie du projet
*  OpenCADC project.                    OpenCADC.
*
*  OpenCADC is free software:           OpenCADC est un logiciel libre ;
*  you can redistribute it and/or       vous pouvez le redistribuer ou le
*  modify it under the terms of         modifier suivant les termes de
*  the GNU Affero General Public        la “GNU Affero General Public
*  License as published by the          License” telle que publiée
*  Free Software Foundation,            par la Free Software Foundation
*  either version 3 of the              : soit la version 3 de cette
*  License, or (at your option)         licence, soit (à votre gré)
*  any later version.                   toute version ultérieure.
*
*  OpenCADC is distributed in the       OpenCADC est distribué
*  hope that it will be useful,         dans l’espoir qu’il vous
*  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
*  without even the implied             GARANTIE : sans même la garantie
*  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
*  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
*  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
*  General Public License for           Générale Publique GNU Affero
*  more details.                        pour plus de détails.
*
*  You should have received             Vous devriez avoir reçu une
*  a copy of the GNU Affero             copie de la Licence Générale
*  General Public License along         Publique GNU Affero avec
*  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
*  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
*                                       <http://www.gnu.org/licenses/>.
*
************************************************************************
 */

package org.opencadc.inventory;

import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.util.Log4jInit;
import ca.nrc.cadc.vodml.SchematronValidationException;
import ca.nrc.cadc.vodml.VOModelReader;
import ca.nrc.cadc.vodml.VOModelWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author pdowler
 */
public class VODMLValidationTest {

    private static final Logger log = Logger.getLogger(VODMLValidationTest.class);

    private static final String VODML_FILE_01 = "storage-inventory-0.1-vodml.xml";
    private static final String VODML_FILE_02 = "storage-inventory-0.2-vodml.xml";
    private static final String VODML_FILE_03 = "storage-inventory-0.3-vodml.xml";
    private static final String VODML_FILE_04 = "storage-inventory-0.4-vodml.xml";
    private static final String VODML_FILE_05 = "storage-inventory-0.5-vodml.xml";
    private static final String VODML_FILE_06 = "storage-inventory-0.6-vodml.xml";

    private static final String[] VODML_FILES = new String[]{
        VODML_FILE_01, VODML_FILE_02, VODML_FILE_03, VODML_FILE_04, VODML_FILE_05, VODML_FILE_06
    };

    static {
        Log4jInit.setLevel("org.opencadc.inventory", Level.INFO);
        Log4jInit.setLevel("ca.nrc.cadc.vodml", Level.INFO);
    }

    public VODMLValidationTest() {
    }

    @Test
    public void testWellFormed() {
        for (String vodmlFile : VODML_FILES) {
            try {
                File testVODML = FileUtil.getFileFromResource(vodmlFile, VODMLValidationTest.class);
                log.info("testWellFormed VO-DML/XML doc: " + testVODML);

                VOModelReader wf = new VOModelReader(false, false, false);
                Document doc = wf.read(new FileInputStream(testVODML));
                Assert.assertNotNull(doc);

                VOModelWriter w = new VOModelWriter();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                w.write(doc, bos);
                log.debug("well-formed document:\n" + bos.toString());
                log.info("testWellFormed VO-DML/XML doc: OK");
            } catch (Exception unexpected) {
                log.error("unexpected exception", unexpected);
                Assert.fail("unexpected exception: " + unexpected);
            }
        }
    }

    @Test
    public void testSchemaValid() {
        for (String vodmlFile : VODML_FILES) {
            try {
                File testVODML = FileUtil.getFileFromResource(vodmlFile, VODMLValidationTest.class);
                log.info("testSchemaValid VO-DML/XML doc: " + testVODML);

                VOModelReader wf = new VOModelReader(true, false, false);
                Document doc = wf.read(new FileInputStream(testVODML));
                Assert.assertNotNull(doc);

                VOModelWriter w = new VOModelWriter();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                w.write(doc, bos);
                log.debug("schema-valid document:\n" + bos.toString());
                log.info("testSchemaValid VO-DML/XML doc: OK");

            } catch (Exception unexpected) {
                log.error("unexpected exception", unexpected);
                Assert.fail("unexpected exception: " + unexpected);
            }
        }
    }

    @Test
    public void testSchematronValid() {
        for (String vodmlFile : VODML_FILES) {
            try {
                File testVODML = FileUtil.getFileFromResource(vodmlFile, VODMLValidationTest.class);
                log.info("testSchematronValid VO-DML/XML doc: " + testVODML);

                VOModelReader wf = new VOModelReader(true, true, true);
                Document doc = wf.read(new FileInputStream(testVODML));
                Assert.assertNotNull(doc);
                log.info("testSchematronValid VO-DML/XML doc: OK");
            } catch (SchematronValidationException ex) {
                for (String msg : ex.getFailures()) {
                    log.error(msg);
                }
                Assert.fail("schematron validation failed: " + ex);
            } catch (Exception unexpected) {
                log.error("unexpected exception", unexpected);
                Assert.fail("unexpected exception: " + unexpected);
            }
        }
    }
}
