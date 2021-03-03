/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2021.                            (c) 2021.
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
 *  : 5 $
 *
 ************************************************************************
 */

package org.opencadc.inventory.query;

import ca.nrc.cadc.io.ResourceIterator;
import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.net.TransientException;
import ca.nrc.cadc.util.StringUtil;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.opencadc.inventory.DeletedArtifactEvent;
import org.opencadc.inventory.InventoryUtil;
import org.opencadc.tap.TapClient;
import org.opencadc.tap.TapRowMapper;

public class DeletedArtifactEventQuery {
    private static final Logger log = Logger.getLogger(DeletedArtifactEventQuery.class);

    /**
     * Default constructor.
     */
    public DeletedArtifactEventQuery() {}

    /**
     * Run a TAP query against a TAP service specified by the resourceID,
     * for DeletedArtifactEvent's that match the given where clause.
     *
     * @param where The query where clause.
     * @param resourceID The resourceID of the query service.
     * @return ResourceIterator over DeletedArtifactEvent's matching the where clause.
     *
     * @throws ResourceNotFoundException For any missing required configuration that is missing.
     * @throws IOException               For unreadable configuration files.
     * @throws TransientException        temporary failure of TAP service: same call could work in future
     * @throws InterruptedException      thread interrupted
     */
    public ResourceIterator<DeletedArtifactEvent> execute(final String where, final URI resourceID)
        throws ResourceNotFoundException, IOException, InterruptedException, TransientException {
        final TapClient<DeletedArtifactEvent> tapClient = new TapClient<>(resourceID);
        final String query = getQuery(where);
        log.debug("\nExecuting query '" + query + "'\n");
        return tapClient.execute(query, getRowMapper());
    }

    /**
     * Assemble a query to the DeletedArtifactEvent table with the given where clause.
     *
     * @param where The query where clause.
     * @return  String query.  Never null.
     */
    protected String getQuery(final String where) {
        final StringBuilder query = new StringBuilder();
        query.append("SELECT id, lastModified, metaChecksum FROM inventory.DeletedArtifactEvent ");
        if (StringUtil.hasText(where)) {
            query.append(where);
        }
        // TODO sort?
        return query.toString();
    }

    /**
     * Get a RowMapper for a DeletedArtifactEvent.
     *
     * @return DeletedArtifactEvent RowMapper.
     */
    protected DeletedArtifactEventRowMapper getRowMapper() {
        return new DeletedArtifactEventRowMapper();
    }

    /**
     * Class to map the query results to a DeletedArtifactEvent.
     */
    static class DeletedArtifactEventRowMapper implements TapRowMapper<DeletedArtifactEvent> {

        @Override
        public DeletedArtifactEvent mapRow(List<Object> row) {
            int index = 0;
            final UUID id = (UUID) row.get(index++);

            final DeletedArtifactEvent deletedArtifactEvent = new DeletedArtifactEvent(id);
            InventoryUtil.assignLastModified(deletedArtifactEvent, (Date) row.get(index++));
            InventoryUtil.assignMetaChecksum(deletedArtifactEvent, (URI) row.get(index));
            return deletedArtifactEvent;
        }
    }

}
