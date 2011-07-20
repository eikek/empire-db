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
package org.apache.empire.db.oracle;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

import org.apache.empire.commons.EmpireException;
import org.apache.empire.commons.Errors;
import org.apache.empire.data.DataType;
import org.apache.empire.db.DBCmdType;
import org.apache.empire.db.DBColumn;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBCommandExpr;
import org.apache.empire.db.DBDatabase;
import org.apache.empire.db.DBDatabaseDriver;
import org.apache.empire.db.DBDriverFeature;
import org.apache.empire.db.DBErrors;
import org.apache.empire.db.DBExpr;
import org.apache.empire.db.DBIndex;
import org.apache.empire.db.DBObject;
import org.apache.empire.db.DBReader;
import org.apache.empire.db.DBRelation;
import org.apache.empire.db.DBSQLScript;
import org.apache.empire.db.DBTable;
import org.apache.empire.db.DBTableColumn;
import org.apache.empire.db.DBView;


/**
 * This class provides support for the Oracle database system.<br>
 * Oracle Version 9 or higher is required.
 */
public class DBDatabaseDriverOracle extends DBDatabaseDriver
{
    private static final long serialVersionUID = 1L;


    // Implementation of boolean types
    public enum BooleanType
    {
        CHAR,       // as CHAR(1) with 'Y' for true and 'N' for false
        NUMBER      // as NUMBER(1) with 1 for true and 0 for false
    }
    
    private boolean oracle8Compatibilty = false;

    private BooleanType booleanType = BooleanType.NUMBER;

    /**
     * Constructor for the Oracle database driver.<br>
     * 
     */
    public DBDatabaseDriverOracle()
    {
        // Info
        log.info("DBDatabaseDriverOracle created. Boolean Type is " + booleanType);
    }

    public boolean isOracle8Compatibilty()
    {
        return oracle8Compatibilty;
    }

    public void setOracle8Compatibilty(boolean oracle8Compatibilty)
    {
        this.oracle8Compatibilty = oracle8Compatibilty;
    }

    public BooleanType getBooleanType()
    {
        return booleanType;
    }

    public void setBooleanType(BooleanType booleanType)
    {
        this.booleanType = booleanType;
        log.info("DBDatabaseDriverOracle Boolean Type set to " + booleanType);
    }

    /**
     * Returns whether or not a particular feature is supported by this driver
     * @param type type of requested feature. @see DBDriverFeature
     * @return true if the features is supported or false otherwise
     */
    @Override
    public boolean isSupported(DBDriverFeature type)
    {
        switch (type)
        {   // return support info 
            case CREATE_SCHEMA: 	return false;
            case SEQUENCES:     	return true;
            case QUERY_LIMIT_ROWS:  return true;
            case QUERY_SKIP_ROWS:   return false;
            default:
                // All other features are not supported by default
                return false;
        }
    }

    /**
     * Creates a new Oracle command object.
     * 
     * @return the new DBCommandOracle object
     */
    @Override
    public DBCommand createCommand(DBDatabase db)
    {
        if (db == null)
            return null;
        // create oracle command
        return new DBCommandOracle(db);
    }

    /**
     * Gets an sql phrase template for this database system.<br>
     * @see DBDatabaseDriver#getSQLPhrase(int)
     * @return the phrase template
     */
    @Override
    public String getSQLPhrase(int phrase)
    {
        switch (phrase)
        {
            // sql-phrases
            case SQL_NULL_VALUE:                return "null";
            case SQL_PARAMETER:                 return " ? ";
            case SQL_RENAME_TABLE:              return " ";
            case SQL_RENAME_COLUMN:             return " AS ";
            case SQL_DATABASE_LINK:             return "@";
            case SQL_QUOTES_OPEN:               return "\"";
            case SQL_QUOTES_CLOSE:              return "\"";
            case SQL_CONCAT_EXPR:               return " || ";
            // data types
            case SQL_BOOLEAN_TRUE:              return (booleanType==BooleanType.CHAR) ? "'Y'" : "1";
            case SQL_BOOLEAN_FALSE:             return (booleanType==BooleanType.CHAR) ? "'N'" : "0";
            case SQL_CURRENT_DATE:              return "sysdate";
            case SQL_DATE_PATTERN:              return "yyyy-MM-dd";
            case SQL_DATE_TEMPLATE:             return "TO_DATE('{0}', 'YYYY-MM-DD')";
            case SQL_CURRENT_DATETIME:          return "sysdate";
            case SQL_DATETIME_PATTERN:          return "yyyy-MM-dd HH:mm:ss";
            case SQL_DATETIME_TEMPLATE:         return "TO_DATE('{0}', 'YYYY-MM-DD HH24:MI:SS')";
            // functions
            case SQL_FUNC_COALESCE:             return "nvl(?, {0})";
            case SQL_FUNC_SUBSTRING:            return "substr(?, {0})";
            case SQL_FUNC_SUBSTRINGEX:          return "substr(?, {0}, {1})";
            case SQL_FUNC_REPLACE:              return "replace(?, {0}, {1})";
            case SQL_FUNC_REVERSE:              return "reverse(?)"; 
            case SQL_FUNC_STRINDEX:             return "instr(?, {0})"; 
            case SQL_FUNC_STRINDEXFROM:         return "instr(?, {0}, {1})"; 
            case SQL_FUNC_LENGTH:               return "length(?)";
            case SQL_FUNC_UPPER:                return "upper(?)";
            case SQL_FUNC_LOWER:                return "lower(?)";
            case SQL_FUNC_TRIM:                 return "trim(?)";
            case SQL_FUNC_LTRIM:                return "ltrim(?)";
            case SQL_FUNC_RTRIM:                return "rtrim(?)";
            case SQL_FUNC_ESCAPE:               return "? escape '{0}'";
            // Numeric
            case SQL_FUNC_ABS:                  return "abs(?)";
            case SQL_FUNC_ROUND:                return "round(?,{0})";
            case SQL_FUNC_TRUNC:                return "trunc(?,{0})";
            case SQL_FUNC_CEILING:              return "ceil(?)";
            case SQL_FUNC_FLOOR:                return "floor(?)";
            // Date
            case SQL_FUNC_DAY:                  return oracle8Compatibilty ? "to_number(to_char(?,'DD'))"   : "extract(day from ?)";
            case SQL_FUNC_MONTH:                return oracle8Compatibilty ? "to_number(to_char(?,'MM'))"   : "extract(month from ?)";
            case SQL_FUNC_YEAR:                 return oracle8Compatibilty ? "to_number(to_char(?,'YYYY'))" : "extract(year from ?)";
            // Aggregation
            case SQL_FUNC_SUM:                  return "sum(?)";
            case SQL_FUNC_MAX:                  return "max(?)";
            case SQL_FUNC_MIN:                  return "min(?)";
            case SQL_FUNC_AVG:                  return "avg(?)";
            // Others
            case SQL_FUNC_DECODE:               return "decode(? {0})";
            case SQL_FUNC_DECODE_SEP:           return ",";
            case SQL_FUNC_DECODE_PART:          return "{0}, {1}";
            case SQL_FUNC_DECODE_ELSE:          return "{0}";
            // Not defined
            default:
                log.error("SQL phrase " + phrase + " is not defined!");
                return "?";
        }
    }

    /**
     * @see DBDatabaseDriver#getConvertPhrase(DataType, DataType, Object)
     */
    @Override
    public String getConvertPhrase(DataType destType, DataType srcType, Object format)
    {
        switch (destType)
        {
            /*
             * case DBExpr.DT_BOOL: return "convert(bit, ?)"; case DBExpr.DT_INTEGER: return "convert(int, ?)"; case
             * DBExpr.DT_DECIMAL: return "convert(decimal, ?)"; case DBExpr.DT_NUMBER: return "convert(float, ?)"; case
             * DBExpr.DT_DATE: return "convert(datetime, ?, 111)"; case DBExpr.DT_DATETIME: return "convert(datetime, ?, 120)";
             */
            // Convert to text
            case TEXT:
            case CHAR:
            case CLOB:
                if (format != null)
                { // Convert using a format string
                    return "to_char(?, '"+format.toString()+"')";
                }
                return "to_char(?)";
            // Convert to number
            case INTEGER:
            case DOUBLE:
            case DECIMAL:
                if (format != null)
                { // Convert using a format string
                    return "to_number(?, '"+format.toString()+"')";
                }
                return "to_number(?)";
            // Convert to date
            case DATE:
            case DATETIME:
                if (format != null)
                { // Convert using a format string
                    return "to_date(?, '"+format.toString()+"')";
                }
                return "to_date(?)";
            // Unknown Type
            default:
                log.error("getConvertPhrase: unknown type " + destType);
                return "?";
        }
    }

    /**
     * Extracts native error message of an sqlExeption.
     * 
     * @param sqle the SQLException
     * @return the error message of the database 
     */
    @Override
    public String extractErrorMessage(SQLException sqle)
    {
        String msg = sqle.getMessage();
        msg = msg.substring(msg.indexOf(':') + 1);
        // Find End
        int end = msg.indexOf("ORA");
        if (end >= 0)
            msg = msg.substring(0, end - 1);
        return msg;
    }
    
    /**
     * Gets the value of a sql ResultSet.
     * Gives the driver the oportunity to change the value
     * i.e. to simulate missing data types with other types.
     * 
     * @param rset the sql Resultset with the current data row
     * @param columnIndex one based column Index of the desired column
     * @param dataType the desired data type
     * @return the value of the column 
     */
    @Override
    public Object getResultValue(ResultSet rset, int columnIndex, DataType dataType)
        throws SQLException
    {
        // Check for character large object
        if (dataType == DataType.BOOL)
        {   // Get character large object
            String val = rset.getString(columnIndex);
            if (val==null || rset.wasNull())
                return null;
            // Check Value
            if (val.equalsIgnoreCase("Y") || val.equals("1"))
                return Boolean.TRUE;
            return Boolean.FALSE;    
        }
        // Default
        return super.getResultValue(rset, columnIndex, dataType);
    }

    /**
     * @see DBDatabaseDriver#getNextSequenceValue(DBDatabase, String, int, Connection)
     */
    @Override
    public Object getNextSequenceValue(DBDatabase db, String seqName, int minValue, Connection conn)
    { // Use Oracle Sequences
        StringBuilder sql = new StringBuilder(80);
        sql.append("SELECT ");
        db.appendQualifiedName(sql, seqName, detectQuoteName(seqName));
        sql.append(".NEXTVAL FROM DUAL");
        Object val = db.querySingleValue(sql.toString(), conn);
        if (val == null)
        { // Error!
            log.error("getNextSequenceValue: Invalid sequence value for sequence " + seqName);
        }
        // Done
        return val;
    }

    /**
     * @see DBDatabaseDriver#getDDLScript(DBCmdType, DBObject, DBSQLScript)  
     */
    @Override
    public void getDDLScript(DBCmdType type, DBObject dbo, DBSQLScript script)
    {
        // The Object's database must be attached to this driver
        if (dbo==null || dbo.getDatabase().getDriver()!=this)
            throw new EmpireException(Errors.InvalidArg, dbo, "dbo");
        // Check Type of object
        if (dbo instanceof DBDatabase)
        { // Database
            switch (type)
            {
                case CREATE:
                    createDatabase((DBDatabase) dbo, script);
                    return;
                case DROP:
                    dropObject(((DBDatabase) dbo).getSchema(), "USER", script);
                    return;
                default:
                    throw new EmpireException(Errors.NotImplemented, "getDDLScript." + dbo.getClass().getName() + "." + type);
            }
        } 
        else if (dbo instanceof DBTable)
        { // Table
            switch (type)
            {
                case CREATE:
                    createTable((DBTable) dbo, script);
                    return;
                case DROP:
                    dropObject(((DBTable) dbo).getName(), "TABLE", script);
                    return;
                default:
                    throw new EmpireException(Errors.NotImplemented, "getDDLScript." + dbo.getClass().getName() + "." + type);
            }
        } 
        else if (dbo instanceof DBView)
        { // View
            switch (type)
            {
                case CREATE:
                    createView((DBView) dbo, script);
                    return;
                case DROP:
                    dropObject(((DBView) dbo).getName(), "VIEW", script);
                    return;
                default:
                    throw new EmpireException(Errors.NotImplemented, "getDDLScript." + dbo.getClass().getName() + "." + type);
            }
        } 
        else if (dbo instanceof DBRelation)
        { // Relation
            switch (type)
            {
                case CREATE:
                    createRelation((DBRelation) dbo, script);
                    return;
                case DROP:
                    dropObject(((DBRelation) dbo).getName(), "CONSTRAINT", script);
                    return;
                default:
                    throw new EmpireException(Errors.NotImplemented, "getDDLScript." + dbo.getClass().getName() + "." + type);
            }
        } 
        else if (dbo instanceof DBTableColumn)
        { // Table Column
            alterTable((DBTableColumn) dbo, type, script);
            return;
        } 
        else
        { // an invalid argument has been supplied
            throw new EmpireException(Errors.InvalidArg, dbo, "dbo");
        }
    }

    /**
     * Overridden. Returns a timestamp that is used for record updates created by the database server.
     * 
     * @return the current date and time of the database server.
     */
    @Override
    public java.sql.Timestamp getUpdateTimestamp(Connection conn)
    {
        // Default implementation
        ResultSet rs = null;
        try
        {   // Oracle Timestamp query
            rs = executeQuery("select sysdate from dual", null, false, conn);
            return (rs.next() ? rs.getTimestamp(1) : null);
        } catch (SQLException e) {
            // throw exception
            String msg = extractErrorMessage(e);
            throw new EmpireException(DBErrors.SQLException, new Object[] { msg }, e);
        } finally
        { // Cleanup
            try
            { // ResultSet close
                Statement stmt = (rs!=null) ?  rs.getStatement() : null;
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
            } catch (SQLException e) {
                // throw exception
                String msg = extractErrorMessage(e);
                throw new EmpireException(DBErrors.SQLException, new Object[] { msg }, e);
            }
        }
    }

    /**
     * Returns true if the database has been created successfully.
     * 
     * @return true if the database has been created successfully
     */
    protected void createDatabase(DBDatabase db, DBSQLScript script)
    {
        // Create all Sequences
        Iterator<DBTable> seqtabs = db.getTables().iterator();
        while (seqtabs.hasNext())
        {
            DBTable table = seqtabs.next();
            Iterator<DBColumn> cols = table.getColumns().iterator();
            while (cols.hasNext())
            {
                DBTableColumn c = (DBTableColumn) cols.next();
                if (c.getDataType() == DataType.AUTOINC)
                {
                    createSequence(db, c, script);
                }
            }
        }
        // Create all Tables
        Iterator<DBTable> tables = db.getTables().iterator();
        while (tables.hasNext())
        {
            createTable(tables.next(), script);
        }
        // Create Relations
        Iterator<DBRelation> relations = db.getRelations().iterator();
        while (relations.hasNext())
        {
            createRelation(relations.next(), script);
        }
        // Create Views
        Iterator<DBView> views = db.getViews().iterator();
        while (views.hasNext())
        {
            try {
                createView(views.next(), script);
            } catch(EmpireException e) {
                // Error
                if (e.getErrorType()!=Errors.NotImplemented)
                {   // View command not implemented
                    log.warn("Error creating the view {0}. This view will be ignored.");
                    continue;
                }    
                else throw e; 
            }
        }
    }

    /**
     * Returns true if the sequence has been created successfully.
     * 
     * @return true if the sequence has been created successfully
     */
    protected void createSequence(DBDatabase db, DBTableColumn c, DBSQLScript script)
    {
        Object defValue = c.getDefaultValue();
        String seqName = (defValue != null) ? defValue.toString() : c.toString();
        // createSQL
        StringBuilder sql = new StringBuilder();
        sql.append("-- creating sequence for column ");
        sql.append(c.getFullName());
        sql.append(" --\r\n");
        sql.append("CREATE SEQUENCE ");
        db.appendQualifiedName(sql, seqName, detectQuoteName(seqName));
        sql.append(" INCREMENT BY 1 START WITH 1 MINVALUE 0 NOCYCLE NOCACHE NOORDER");
        // executeDLL
        script.addStmt(sql);
    }
    
    /**
     * Returns true if the table has been created successfully.
     * @return true if the table has been created successfully
     */
    protected void createTable(DBTable t, DBSQLScript script)
    {
        StringBuilder sql = new StringBuilder();
        sql.append("-- creating table ");
        sql.append(t.getName());
        sql.append(" --\r\n");
        sql.append("CREATE TABLE ");
        t.addSQL(sql, DBExpr.CTX_FULLNAME);
        sql.append(" (");
        boolean addSeparator = false;
        Iterator<DBColumn> columns = t.getColumns().iterator();
        while (columns.hasNext())
        {
            DBTableColumn c = (DBTableColumn) columns.next();
            sql.append((addSeparator) ? ",\r\n   " : "\r\n   ");
            if (appendColumnDesc(c, sql)==false)
                continue; // Ignore and continue;
            addSeparator = true;
        }
        // Primary Key
        DBIndex pk = t.getPrimaryKey();
        if (pk != null)
        { // add the primary key
            sql.append(",\r\n CONSTRAINT ");
            appendElementName(sql, pk.getName());
            sql.append(" PRIMARY KEY (");
            addSeparator = false;
            // columns
            DBColumn[] keyColumns = pk.getColumns();
            for (int i = 0; i < keyColumns.length; i++)
            {
                sql.append((addSeparator) ? ", " : "");
                keyColumns[i].addSQL(sql, DBExpr.CTX_NAME);
                addSeparator = true;
            }
            sql.append(")");
        }
        sql.append(")");
        // Create the table
        DBDatabase db = t.getDatabase();
        script.addStmt(sql);
        // Create other Indexes (except primary key)
        Iterator<DBIndex> indexes = t.getIndexes().iterator();
        while (indexes.hasNext())
        {
            DBIndex idx = indexes.next();
            if (idx == pk || idx.getType() == DBIndex.PRIMARYKEY)
                continue;

            // Cretae Index
            sql.setLength(0);
            sql.append((idx.getType() == DBIndex.UNIQUE) ? "CREATE UNIQUE INDEX " : "CREATE INDEX ");
            appendElementName(sql, idx.getName());
            sql.append(" ON ");
            t.addSQL(sql, DBExpr.CTX_FULLNAME);
            sql.append(" (");
            addSeparator = false;

            // columns
            DBColumn[] idxColumns = idx.getColumns();
            for (int i = 0; i < idxColumns.length; i++)
            {
                sql.append((addSeparator) ? ", " : "");
                idxColumns[i].addSQL(sql, DBExpr.CTX_NAME);
                sql.append("");
                addSeparator = true;
            }
            sql.append(")");
            // Create Index
            script.addStmt(sql);
        }
        // add Comments
        createComment(db, "TABLE", t, t.getComment(), script);
        columns = t.getColumns().iterator();
        while (columns.hasNext())
        {
            DBColumn c = columns.next();
            String com = c.getComment();
            if (com != null)
                createComment(db, "COLUMN", c, com, script);
        }
    }
    
    /**
     * Appends a table column definition to a ddl statement
     * @param c the column which description to append
     * @param sql the sql builder object
     * @return true if the column was successfully appended or false otherwise
     */
    protected boolean appendColumnDesc(DBTableColumn c, StringBuilder sql)
    {
        // Append name
        c.addSQL(sql, DBExpr.CTX_NAME);
        sql.append(" ");
        switch (c.getDataType())
        {
            case INTEGER:
                sql.append("INTEGER");
                break;
            case AUTOINC:
                sql.append("INTEGER");
                break;
            case TEXT:
            { // Check fixed or variable length
                int size = Math.abs((int) c.getSize());
                if (size == 0)
                    size = 100;
                sql.append("VARCHAR2(");
                sql.append(String.valueOf(size));
                sql.append(" char)");
            }
                break;
            case CHAR:
            { // Check fixed or variable length
                int size = Math.abs((int) c.getSize());
                if (size == 0)
                    size = 1;
                sql.append("CHAR(");
                sql.append(String.valueOf(size));
                sql.append(")");
            }
                break;
            case DATE:
                sql.append("DATE");
                break;
            case DATETIME:
                sql.append("DATE");
                break;
            case BOOL:
                if ( booleanType==BooleanType.CHAR )
                     sql.append("CHAR(1)");
                else sql.append("NUMBER(1,0)");
                break;
            case DOUBLE:
                sql.append("FLOAT(80)");
                break;
            case DECIMAL:
            {
                sql.append("NUMBER(");
                int prec = (int) c.getSize();
                int scale = (int) ((c.getSize() - prec) * 10 + 0.5);
                // sql.append((prec+scale).ToString());sql.append(",");
                sql.append(String.valueOf(prec));
                sql.append(",");
                sql.append(String.valueOf(scale));
                sql.append(")");
            }
                break;
            case CLOB:
                sql.append("CLOB");
                break;
            case BLOB:
                sql.append("BLOB");
                if (c.getSize() > 0)
                    sql.append(" (" + (long) c.getSize() + ") ");
                break;
            case UNIQUEID:
                // emulate using java.util.UUID
                sql.append("CHAR(36)");
                break;
            case UNKNOWN:
                log.warn("Cannot append column of Data-Type 'UNKNOWN'");
                return false;
        }
        // Default Value
        if (isDDLColumnDefaults() && !c.isAutoGenerated() && c.getDefaultValue()!=null)
        {   sql.append(" DEFAULT ");
            sql.append(getValueString(c.getDefaultValue(), c.getDataType()));
        }
        // Nullable
        if (c.isRequired() ||  c.isAutoGenerated())
            sql.append(" NOT NULL");
        // Done
        return true;
    }

    /**
     * Create a sql string for creating a relation and appends it to the supplied buffer
     * @return true if the relation has been created successfully
     */
    protected void createRelation(DBRelation r, DBSQLScript script)
    {
        DBTable sourceTable = (DBTable) r.getReferences()[0].getSourceColumn().getRowSet();
        DBTable targetTable = (DBTable) r.getReferences()[0].getTargetColumn().getRowSet();

        StringBuilder sql = new StringBuilder();
        sql.append("-- creating foreign key constraint ");
        sql.append(r.getName());
        sql.append(" --\r\n");
        sql.append("ALTER TABLE ");
        sourceTable.addSQL(sql, DBExpr.CTX_FULLNAME);
        sql.append(" ADD CONSTRAINT ");
        appendElementName(sql, r.getName());
        sql.append(" FOREIGN KEY (");
        // Source Names
        boolean addSeparator = false;
        DBRelation.DBReference[] refs = r.getReferences();
        for (int i = 0; i < refs.length; i++)
        {
            sql.append((addSeparator) ? ", " : "");
            refs[i].getSourceColumn().addSQL(sql, DBExpr.CTX_NAME);
            addSeparator = true;
        }
        // References
        sql.append(") REFERENCES ");
        targetTable.addSQL(sql, DBExpr.CTX_FULLNAME);
        sql.append(" (");
        // Target Names
        addSeparator = false;
        for (int i = 0; i < refs.length; i++)
        {
            sql.append((addSeparator) ? ", " : "");
            refs[i].getTargetColumn().addSQL(sql, DBExpr.CTX_NAME);
            addSeparator = true;
        }
        // done
        sql.append(")");
        // done
        script.addStmt(sql);
    }

    /**
     * Creates an alter table dll statement for adding, modifiying or droping a column.
     * @param col the column which to add, modify or drop
     * @param type the type of operation to perform
     * @param script to which to append the sql statement to
     * @return true if the statement was successfully appended to the buffer
     */
    protected void alterTable(DBTableColumn col, DBCmdType type, DBSQLScript script)
    {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ");
        col.getRowSet().addSQL(sql, DBExpr.CTX_FULLNAME);
        switch(type)
        {
            case CREATE:
                sql.append(" ADD ");
                appendColumnDesc(col, sql);
                break;
            case ALTER:
                sql.append(" MODIFY ");
                appendColumnDesc(col, sql);
                break;
            case DROP:
                sql.append(" DROP COLUMN ");
                sql.append(col.getName());
                break;
        }
        // done
        script.addStmt(sql);
    }
    
    /**
     * Returns true if the view has been created successfully.
     * 
     * @return true if the view has been created successfully
     */
    protected void createView(DBView v, DBSQLScript script)
    {
        // Create the Command
        DBCommandExpr cmd = v.createCommand();
        if (cmd==null)
        {   // Check whether Error information is available
            log.error("No command has been supplied for view " + v.getName());
            throw new EmpireException(Errors.NotImplemented, v.getName() + ".createCommand");
        }
        // Make sure there is no OrderBy
        cmd.clearOrderBy();

        // Build String
        StringBuilder sql = new StringBuilder();
        sql.append( "CREATE OR REPLACE VIEW ");
        v.addSQL(sql, DBExpr.CTX_FULLNAME);
        sql.append( " (" );
        boolean addSeparator = false;
        for(DBColumn c : v.getColumns())
        {
            if (addSeparator)
                sql.append(", ");
            // Add Column name
            c.addSQL(sql, DBExpr.CTX_NAME);
            // next
            addSeparator = true;
        }
        sql.append(")\r\nAS\r\n");
        cmd.addSQL( sql, DBExpr.CTX_DEFAULT);
        // done
        script.addStmt(sql.toString());
    }

    /**
     * Returns true if the comment has been created successfully.
     * 
     * @return true if the comment has been created successfully
     */
    protected void createComment(DBDatabase db, String type, DBExpr expr, String comment, DBSQLScript script)
    {
        if (comment==null || comment.length()==0)
            return; // Nothing to do
        StringBuilder sql = new StringBuilder();
        sql.append("COMMENT ON ");
        sql.append(type);
        sql.append(" ");
        if (expr instanceof DBColumn)
        {
            DBColumn c = (DBColumn)expr;
            c.getRowSet().addSQL(sql, DBExpr.CTX_NAME);
            sql.append(".");
        }
        expr.addSQL(sql, DBExpr.CTX_NAME);
        sql.append(" IS '");
        sql.append(comment);
        sql.append("'");
        // Create Index
        script.addStmt(sql);
    }
    
    /**
     * Returns true if the object has been dropped successfully.
     * 
     * @return true if the object has been dropped successfully
     */
    protected void dropObject(String name, String objType, DBSQLScript script)
    {
        if (name == null || name.length() == 0)
            throw new EmpireException(Errors.InvalidArg, name, "name");
        // Create Drop Statement
        StringBuilder sql = new StringBuilder();
        sql.append("DROP ");
        sql.append(objType);
        sql.append(" ");
        appendElementName(sql, name);
        script.addStmt(sql);
    }

    
    /**
     * Checks whether the database definition matches the real database structure.
     * 
     * @return true if the database definition matches the real database structure.
     */
    @Override
    public void checkDatabase(DBDatabase db, String owner, Connection conn)
    {
        // Check Params
        if (owner==null || owner.length()==0)
            throw new EmpireException(Errors.InvalidArg, owner, "owner");
        // Database definition
        OracleSYSDatabase sysDB = new OracleSYSDatabase(this);
        // Check Columns
        DBCommand sysDBCommand = sysDB.createCommand();
        sysDBCommand.select(sysDB.CI.getColumns());
        sysDBCommand.where (sysDB.CI.C_OWNER.is(owner));
        
        OracleDataDictionnary dataDictionnary = new OracleDataDictionnary();
        DBReader rd = new DBReader();
        try
        {
            rd.open(sysDBCommand, conn);
            // read all
            log.info("---------------------------------------------------------------------------------");
            log.info("checkDatabase start: " + db.getClass().getName());
            String skipTable = "";
            while (rd.moveNext())
            {
                String tableName = rd.getString(sysDB.CI.C_TABLE_NAME);

                // if a table wasn't found before, skip it
                if (tableName.equals(skipTable))
                    continue;

                // check if the found table exists in the DBDatabase object
                String columnName = rd.getString(sysDB.CI.C_COLUMN_NAME);
                DBTable dbTable = db.getTable(tableName);
                DBView  dbView  = db.getView(tableName);
                
                String dataType = rd.getString(sysDB.CI.C_DATA_TYPE);
                int charLength = rd.getInt(sysDB.CI.C_CHAR_LENGTH);
                int dataLength = rd.getInt(sysDB.CI.C_DATA_LENGTH);
                int dataPrecision = rd.getInt(sysDB.CI.C_DATA_PRECISION);
                int dataScale = rd.getInt(sysDB.CI.C_DATA_SCALE);
                String nullable = rd.getString(sysDB.CI.C_NULLABLE);
                
                dataDictionnary.fillDataDictionnary(tableName, columnName, dataType, 
                                                    charLength, dataLength, dataPrecision, dataScale, nullable);
                
                if (dbTable != null)
                {
                    
                    // check if the found column exists in the found DBTable
                    DBColumn col = dbTable.getColumn(columnName);
                    if (col == null)
                    {
                        log.warn("COLUMN NOT FOUND IN " + db.getClass().getName() + "\t: [" + tableName + "]["
                                       + columnName + "][" + dataType + "][" + dataLength + "]");
                        continue;
                    }
                    /*
                    else
                    {   // check the DBTableColumn definition
                        int length = (charLength>0) ? charLength : dataLength;
                        dataDictionnary.checkColumnDefinition(col, dataType, length, dataPrecision, dataScale, nullable.equals("N"));
                    }
                    */
                } 
                else if (dbView!=null)
                {
                    log.debug("Column check for view " + tableName + " not yet implemented.");
                } 
                else
                {
                    log.debug("TABLE OR VIEW NOT FOUND IN " + db.getClass().getName() + "\t: [" + tableName + "]");
                    // skip this table
                    skipTable = tableName;
                    continue;
                }
            }
            // check Tables
            dataDictionnary.checkDBTableDefinition(db.getTables());
            // check Views
            dataDictionnary.checkDBViewDefinition (db.getViews());
            // done
            log.info("checkDatabase end: " + db.getClass().getName());
            log.info("---------------------------------------------------------------------------------");
        } finally {
            // close
            rd.close();
        }
    }

}

