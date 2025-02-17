/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2020.                            (c) 2020.
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

package org.opencadc.inventory.db;

import java.net.URI;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 *
 * @author pdowler
 */
public class HarvestStateDAO extends AbstractDAO<HarvestState> {
    private static final Logger log = Logger.getLogger(HarvestStateDAO.class);

    // update buffer
    private int updateBufferCount = 0;
    private HarvestState bufferedState;
    private int curBufferCount = 0;

    // periodic maintenace
    private int maintCount = -1; // disabled
    private int curMaintCount = 0;
    
    // only usable by itself for testing
    public HarvestStateDAO() { 
        super(true);
    }
    
    public HarvestStateDAO(AbstractDAO src) {
        super(src, true);
    }
    
    /**
     * Set the update buffer count (default: 0). This is the number of calls to
     * <code>put(HarvestState)</code> that are buffered before the value is actually
     * sent to the back end database. For example, if the value is 3 then 3 values will 
     * be buffered and the 4th will be be written. The default (0) means every update is
     * sent to the database.
     * 
     * @param ubc update buffer count
     * @throws IllegalArgumentException if argument is less than 0
     */
    public void setUpdateBufferCount(int ubc) {
        if (ubc < 0) {
            throw new IllegalArgumentException("invalid count: " + ubc + " reason: mujst be [0,)");
        }
        this.updateBufferCount = ubc;
    }
    
    public void flushBufferedState() {
        if (bufferedState != null) {
            super.put(bufferedState);
            bufferedState = null;
            curBufferCount = 0;
        }
    }
    
    /**
     * Set the maintenance frequency (default: -1). The argument count is the number of actual
     * database updates to execute (see setUpdateBufferCount) between maintenance
     * operations. Negative values (like default -1) disable periodic maintenance.
     * 
     * @param mc 
     */
    public void setMaintCount(int mc) {
        this.maintCount = mc;
    }
    
    public HarvestState get(UUID id) {
        HarvestState ret = super.get(HarvestState.class, id);
        return ret;
    }
    
    /**
     * Get HarvestState for the specified name and resourceID (create if necessary).
     * 
     * @param name name of the entity being harvested
     * @param resourceID source of entities
     * @return current (or new) HarvestState (never null)
     */
    public HarvestState get(String name, URI resourceID) {
        if (name == null || resourceID == null) {
            throw new IllegalArgumentException("name and resourceID cannot be null");
        }
        checkInit();
        log.debug("GET: " + name + " " + resourceID);
        long t = System.currentTimeMillis();

        try {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            
            SQLGenerator.HarvestStateGet get = ( SQLGenerator.HarvestStateGet) gen.getEntityGet(HarvestState.class);
            get.setSource(name, resourceID);
            HarvestState o = get.execute(jdbc);
            if (o == null) {
                o = new HarvestState(name, resourceID);
            }
            return o;
        } catch (BadSqlGrammarException ex) {
            handleInternalFail(ex);
        } finally {
            long dt = System.currentTimeMillis() - t;
            log.debug("GET: " + name + " " + resourceID + " " + dt + "ms");
        }
        throw new RuntimeException("BUG: should be unreachable");
    }

    @Override
    public void put(HarvestState val) {
        if (curBufferCount < updateBufferCount) {
            log.debug("buffering: " + curBufferCount + " < " + updateBufferCount + " " + val);
            curBufferCount++;
            bufferedState = val;
        } else {
            super.put(val);
            curBufferCount = 0;
            bufferedState = null;
            
            // only do maintenance after real updates
            if (maintCount > 0) {
                if (curMaintCount == maintCount) {
                    String sql = "VACUUM " + gen.getTable(HarvestState.class);
                    log.warn("maintenance: " + curMaintCount + "==" + maintCount + " " + sql);
                    //JdbcTemplate jdbc = new JdbcTemplate(dataSource);
                    //jdbc.execute(sql);
                    try {
                        dataSource.getConnection().createStatement().execute(sql);
                    } catch (SQLException ex) {
                        log.error("ERROR: " + sql + " FAILED", ex);
                        // yes, log and proceed
                    }
                    curMaintCount = 0;
                } else {
                    log.debug("maintenance: " + curMaintCount + " < " + maintCount);
                    curMaintCount++;
                }
            }
        }
        
        
    }
    
    public void delete(UUID id) {
        super.delete(HarvestState.class, id);
    }
}
