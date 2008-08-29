/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.empire.db.expr.column;

// Java
import java.util.Set;

import org.apache.empire.data.DataType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBRowSet;
import org.apache.empire.xml.XMLUtil;
import org.w3c.dom.Element;


/**
 * This class is used to add the "count" statement to the SQL-Command.
 * <P>
 * There is no need to explicitly create instances of this class.<BR>
 * Instead use {@link DBRowSet#count() }
 * <P>
 *
 */
public class DBCountExpr extends DBColumnExpr
{
    private final DBRowSet rowset;
    private final DBColumnExpr expr;
    
    /**
     * Constructs a DBCountExpr.
     */
    public DBCountExpr(DBRowSet rowset, DBColumnExpr expr)
    {
        this.rowset = rowset;
        this.expr = expr;
    }

    /**
     * Returns the current DBDatabase object.
     * 
     * @return the current DBDatabase object
     */
    @Override
    public DBDatabase getDatabase()
    {
        return rowset.getDatabase();
    }

    /**
     * Returns the data type: DT_INTEGER.
     * 
     * @return the data type: DT_INTEGER
     */
    @Override
    public DataType getDataType()
    {
        return DataType.INTEGER;
    }

    /**
     * Returns the String "count".
     * 
     * @return the String "count"
     */
    @Override
    public String getName()
    {
        return "count";
    }

    /**
     * Returns null.
     * 
     * @return null
     */
    @Override
    public DBColumn getUpdateColumn()
    {
        return null;
    }

    /**
     * Returns true since the count function is an aggregate function.
     * 
     * @return always true
     */
    @Override
    public boolean isAggregate()
    {
        return true;
    }

    /**
     * @see org.apache.empire.db.DBExpr#addReferencedColumns(Set)
     */
    @Override
    public void addReferencedColumns(Set<DBColumn> list)
    {
        if (expr!=null)
            expr.addReferencedColumns(list);
        else
            list.add(rowset.getColumn(0)); // select any column
    }

    /**
     * Creates the SQL-Command adds the String "count(*)" to the SQL-Command.
     * 
     * @param buf the SQL-Command
     * @param context the current SQL-Command context
     */
    @Override
    public void addSQL(StringBuilder buf, long context)
    { 
        buf.append("count(");
        if (expr!=null)
            expr.addSQL(buf, context);
        else
            buf.append("*");
        buf.append(")");
    }

    /** this helper function calls the XMLUtil.addXML(Element, String) method */
    @Override
    public Element addXml(Element parent, long flags)
    {   // Add Expression
        Element elem;
        if (expr!=null)
        {   elem = expr.addXml(parent, flags);
        }
        else
        {   elem = XMLUtil.addElement(parent, "column");
            elem.setAttribute("name", getName());
        }
        elem.setAttribute("function", "count");
        // done
        return elem;
    }

}
