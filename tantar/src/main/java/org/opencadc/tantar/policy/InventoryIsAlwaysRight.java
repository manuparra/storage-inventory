
/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2022.                            (c) 2022.
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
 *
 ************************************************************************
 */

package org.opencadc.tantar.policy;

import ca.nrc.cadc.date.DateUtil;
import java.text.DateFormat;
import java.util.Date;
import org.apache.log4j.Logger;
import org.opencadc.inventory.Artifact;
import org.opencadc.inventory.storage.StorageMetadata;

/**
 * 
 * @author pdowler
 */
public class InventoryIsAlwaysRight extends ResolutionPolicy {
    private static final Logger log = Logger.getLogger(InventoryIsAlwaysRight.class);

    private Date now = new Date();
    
    public InventoryIsAlwaysRight() {
    }

    private boolean delayAction(Date d) {
        return (d != null && now.before(d));
    }
    
    // for test convenience
    protected void resetDelayTimestamp() {
        now = new Date();
    }
    
    /**
     * Use the logic of this Policy to correct a conflict caused by the two given items.  One of the arguments can
     * be null, but not both.
     *
     * @param artifact        The Artifact to use in deciding.
     * @param storageMetadata The StorageMetadata to use in deciding.
     */
    @Override
    public void validate(final Artifact artifact, final StorageMetadata storageMetadata) throws Exception {
        if (artifact == null && storageMetadata == null) {
            throw new RuntimeException("BUG: both args to resolve are null");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName());
        
        Artifact art = artifact;
        
        if (storageMetadata != null && !storageMetadata.isValid()) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append(this.getClass().getSimpleName());
            sb2.append(".deleteStorageLocation");
            if (art != null) {
                sb.append(" Artifact.id=").append(art.getID());
                sb.append(" Artifact.uri=").append(art.getURI());
            }
            sb2.append(" loc=").append(storageMetadata.getStorageLocation());
            sb2.append(" reason=invalid-storageLocation");
            log.info(sb2.toString());
            validateActions.delete(storageMetadata);
        }
        if (storageMetadata == null || !storageMetadata.isValid()) {
            if (art != null) {
                sb.append(".clearStorageLocation");
                sb.append(" Artifact.id=").append(art.getID());
                sb.append(" Artifact.uri=").append(art.getURI());
                sb.append(" loc=").append(art.storageLocation);
                sb.append(" reason=");
                if (storageMetadata == null) {
                    sb.append("no-matching-storageLocation");
                } else {
                    sb.append("invalid-storageLocation");
                }
                log.info(sb.toString());
                validateActions.clearStorageLocation(art);
            }
            return;
        }
        
        //  storageMetadata != null && storageMetadata.isValid()
        if (art == null) {
            // check for existing artifact with unmatched StorageLocation (possibly different bucket)
            art = validateActions.getArtifact(storageMetadata.getArtifactURI());
        }
        if (art == null) {
            if (delayAction(storageMetadata.getContentLastModified())) {
                // delay cleanup in case the object was stored since we started validating
                sb.append(".delayAction");
                sb.append(" Artifact.uri=").append(storageMetadata.getArtifactURI());
                sb.append(" loc=").append(storageMetadata.getStorageLocation());
                sb.append(" reason=no-matching-artifact");
                log.info(sb.toString());
                validateActions.delayAction();
            } else {
                sb.append(".deleteStorageLocation");
                sb.append(" Artifact.uri=").append(storageMetadata.getArtifactURI());
                sb.append(" loc=").append(storageMetadata.getStorageLocation());
                sb.append(" reason=no-matching-artifact");
                log.info(sb.toString());
                validateActions.delete(storageMetadata);
            }
            return;
        }

        // artifact != null 
        // only check metadata if already same storageLocation 
        if (storageMetadata.getStorageLocation().equals(art.storageLocation)
                && !isSameContent(art, storageMetadata)) {
            sb.append(".clearStorageLocation");
            sb.append(" Artifact.id=").append(art.getID());
            sb.append(" Artifact.uri=").append(art.getURI());
            sb.append(" loc=").append(art.storageLocation);
            sb.append(" reason=metadata");
            log.info(sb.toString());
            validateActions.clearStorageLocation(art);

            StringBuilder sb2 = new StringBuilder();
            sb2.append(this.getClass().getSimpleName());
            sb2.append(".deleteStorageLocation");
            sb2.append(" Artifact.id=").append(art.getID());
            sb2.append(" Artifact.uri=").append(art.getURI());
            sb2.append(" loc=").append(storageMetadata.getStorageLocation());
            sb2.append(" reason=metadata");
            log.info(sb2.toString());
            validateActions.delete(storageMetadata);
            return;
        }

        // recover
        if (art.storageLocation == null || !art.storageLocation.equals(storageMetadata.getStorageLocation())) {
            // same file content: fix storage location
            boolean sc = isSameContent(art, storageMetadata);
            boolean doFix = false;
            String reason = "null-storageLocation";
            if (sc) {
                if (art.storageLocation == null) {
                    doFix = true;
                } else if (!art.storageLocation.equals(storageMetadata.getStorageLocation())
                        && art.getContentLastModified().equals(storageMetadata.getContentLastModified())) {
                    if (!storageMetadata.deletePreserved) {
                        // don't recover a previously deleted stored object in this case
                        doFix = true;
                        reason = "improve-contentLastModified";
                    }
                }
            }
            
            if (doFix) {
                sb.append(".updateArtifact");
                sb.append(" Artifact.id=").append(art.getID());
                sb.append(" Artifact.uri=").append(art.getURI());
                sb.append(" loc=").append(storageMetadata.getStorageLocation());
                sb.append(" reason=").append(reason);
                log.info(sb.toString());
                validateActions.updateArtifact(art, storageMetadata);
                return;
            } else {
                if (storageMetadata.deletePreserved) {
                    sb.append(".skip");
                } else {
                    sb.append(".deleteStorageLocation");
                }
                sb.append(" Artifact.id=").append(art.getID());
                sb.append(" Artifact.uri=").append(art.getURI());
                sb.append(" loc=").append(storageMetadata.getStorageLocation());
                sb.append(" reason=old-storageLocation");
                log.info(sb.toString());
                if (!storageMetadata.deletePreserved) {
                    validateActions.delete(storageMetadata);
                }
                return;
            }
        }
        
        sb.append(".valid");
        sb.append(" Artifact.id=").append(art.getID());
        sb.append(" Artifact.uri=").append(art.getURI());
        sb.append(" loc=").append(art.storageLocation);
        // valid gets logged at debug (so probably not)
        log.debug(sb.toString());
    }
}
