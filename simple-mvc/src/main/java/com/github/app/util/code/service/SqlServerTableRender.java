package com.github.app.util.code.service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.github.app.util.code.CodeConfig;
import com.github.app.util.code.model.CodeColumn;
import com.github.app.util.code.model.CodeModule;
import com.github.app.util.code.model.CodeTable;
import com.github.app.util.code.model.CodeTableConf;
import com.github.app.util.code.util.CodeUtil;

public class SqlServerTableRender implements ITableRender {

    private CodeConfig config;

    @Override
    public void setConfig(CodeConfig config) {
        this.config = config;
    }

    /*
     * 连接数据库获取所有表信息 
     */
    @Override
    public List<CodeTableConf> getAllTables(String pattern) {
        if (CodeUtil.isEmpty(pattern)) {
            pattern = "*";
        }
        List<CodeTableConf> tbConfList = new ArrayList<CodeTableConf>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Class.forName(config.getDb().getDriver());
            con = DriverManager
                    .getConnection(config.getDb().getUrl(), config.getDb().getUser(), config.getDb().getPwd());
            // 获取所有表名
            String showTablesSql = "";
            showTablesSql = "SELECT [name] FROM sys.objects ds  where type='U' and [name] like '" + pattern + "'";
            ps = con.prepareStatement(showTablesSql);
            rs = ps.executeQuery();

            // 循环生成所有表的表信息
            while (rs.next()) {
                if (rs.getString(1) == null) {
                    continue;
                }
                CodeTableConf cf = new CodeTableConf();
                cf.setName(rs.getString(1));
                tbConfList.add(cf);
            }

            rs.close();
            ps.close();
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return tbConfList;
    }

    /**
     * 获取指定表信息并封装成Table对象
     *
     * @param tbConf
     * @param module
     * @param con
     */
    @Override
    public CodeTable getTable(CodeTableConf tbConf, CodeModule module, Connection con) throws SQLException {
        String tableName = tbConf.getName();
        CodeTable table = new CodeTable();
        table.setTableFullName(tableName);
        table.setTableName(tableName);
        if (module.isDeleteTablePrefix() && !CodeUtil.isEmpty(tbConf.getPrefix())) {
            table.setTableName(tableName.toLowerCase().replaceFirst(tbConf.getPrefix(), ""));
        }
        System.out.println(tbConf);
        //获取表各字段的信息
        fillTableColumns(table, con);

        table.setPrimaryKey(getTablePrimaryKey(tableName, con));
        table.setPrimaryKeys(getTablePrimaryKeys(tableName, con));
        table.setPrimaryProperty(CodeUtil.convertToFirstLetterLowerCaseCamelCase(table.getPrimaryKey()));
        table.setPrimaryKeyType(getColumnType(table, table.getPrimaryKey()));
        table.setPrimaryPropertyType(CodeUtil.convertType(table.getPrimaryKeyType()));
        table.setPrimaryCamelProperty(CodeUtil.convertToCamelCase(table.getPrimaryKey()));

        table.setRemark(getTableRemark(tableName, con));
        table.setEntityCamelName(CodeUtil.isEmpty(tbConf.getEntityName()) ? CodeUtil.convertToCamelCase(table.getTableName()) : tbConf.getEntityName());
        table.setEntityName(CodeUtil.convertToFirstLetterLowerCaseCamelCase(table.getTableName()));
        table.setModule(module);
        //设置子表的entity属性
        if (!tbConf.getSubTables().isEmpty()) {
            List<CodeTable> subTables = new ArrayList<CodeTable>();
            for (CodeTableConf tc : tbConf.getSubTables()) {
                CodeTable tb = getTable(tc, module, con);
                tb.setParentProperty(CodeUtil.convertToFirstLetterLowerCaseCamelCase(tc.getParentField()));
                tb.setRefType(tc.getRefType());
                subTables.add(tb);
            }
            table.setSubTables(subTables);
        }
        return table;
    }

    /**
     * 获取数据表的所有字段
     *
     * @param table
     * @param conn
     *
     * @throws SQLException
     */
    @Override
    public void fillTableColumns(CodeTable table, Connection conn) throws SQLException {
        //查询表主键
        StringBuffer sb = new StringBuffer();
        sb.append(" SELECT  cast(CASE WHEN col.colorder = 1 THEN obj.name ELSE '' END as varchar(100)) AS table_name , ");
        sb.append("         cast(col.colorder as int) AS column_id ,  ");
        sb.append("         col.name AS column_name ,  ");
        sb.append("         cast (ISNULL(ep.[value], '') as varchar(100)) AS comments ,  ");
        sb.append("         t.name AS data_type ,  ");
        sb.append("         cast (col.length as int) AS data_length ,  ");
        sb.append("         cast(ISNULL(COLUMNPROPERTY(col.id, col.name, 'Scale'), 0) as int) AS precision ,  ");
        sb.append("         cast (CASE WHEN COLUMNPROPERTY(col.id, col.name, 'IsIdentity') = 1 THEN 'Y' ELSE '' END as varchar(3)) AS seq ,  ");
        sb.append("         cast ( CASE WHEN EXISTS ( SELECT   1 FROM     dbo.sysindexes si  ");
        sb.append("                                     INNER JOIN dbo.sysindexkeys sik ON si.id = sik.id AND si.indid = sik.indid  ");
        sb.append("                                     INNER JOIN dbo.syscolumns sc ON sc.id = sik.id AND sc.colid = sik.colid  ");
        sb.append("                                     INNER JOIN dbo.sysobjects so ON so.name = si.name AND so.xtype = 'PK'  ");
        sb.append("                            WHERE sc.id = col.id AND sc.colid = col.colid ) THEN 'Y' ELSE '' END as varchar(3)) AS prim ,  ");
        sb.append("         cast (CASE WHEN col.isnullable = 1 THEN 'Y' ELSE '' END as varchar(3)) AS nullable ,  ");
        sb.append("         cast (ISNULL(comm.text, '') as varchar(30)) AS data_default  ");
        sb.append(" FROM dbo.syscolumns col  ");
        sb.append("         LEFT  JOIN dbo.systypes t ON col.xtype = t.xusertype  ");
        sb.append("         inner JOIN dbo.sysobjects obj ON col.id = obj.id AND obj.xtype = 'U' AND obj.status >= 0  ");
        sb.append("         LEFT  JOIN dbo.syscomments comm ON col.cdefault = comm.id  ");
        sb.append("         LEFT  JOIN sys.extended_properties ep ON col.id = ep.major_id AND col.colid = ep.minor_id AND ep.name = 'MS_Description'  ");
        sb.append(" LEFT  JOIN sys.extended_properties epTwo ON obj.id = epTwo.major_id AND epTwo.minor_id = 0 AND epTwo.name = 'MS_Description'  ");
        sb.append(" WHERE   obj.name = ? ORDER BY col.colorder ; ");
        String sql = sb.toString();
        PreparedStatement ps;
        ResultSet rs;

        ps = conn.prepareStatement(sql);
        ps.setString(1, table.getTableFullName());
        rs = ps.executeQuery();
        while (rs.next()) {
            CodeColumn col = new CodeColumn();
            String colName = rs.getString("column_name");
            col.setColumnName(colName);
            String type = rs.getString("data_type").toUpperCase();
            type = CodeUtil.convertJdbcType(type);
            if (type.equals("NUMERIC")) {
                type = "INTEGER";
            } else if (type.equals("TEXT")) {
                type = "LONGVARCHAR";
            } else if (type.equals("DATETIME")) {
                type = "DATE";
            } else if (type.equals("NVARCHAR")) {
                type = "VARCHAR";
            }
            col.setColumnType(type);
            col.setRemark(rs.getString("comments"));
            col.setPropertyName(CodeUtil.convertToFirstLetterLowerCaseCamelCase(colName));
            col.setPropertyType(CodeUtil.convertType(col.getColumnType()));
            col.setPropertyCamelName(CodeUtil.convertToCamelCase(colName));
            col.setNullable(rs.getString("nullable").equals("Y"));
            col.setLength(rs.getLong("data_length"));
            col.setDefaultValue(rs.getString("data_default"));

            String colKey = rs.getString("prim");
            if ("Y".equals(colKey)) {
                col.setPrimaryKey(true);
            }
            if (col.getPropertyType().indexOf(".") != -1 && !CodeUtil
                    .existsType(table.getImportClassList(), col.getPropertyType())) {
                table.getImportClassList().add(col.getPropertyType());
            }
            col.setIdentity("y".equalsIgnoreCase(rs.getString("seq")));
            table.getColumns().add(col);
        }
        rs.close();
        ps.close();

    }

    @Override
    public String getTablePrimaryKey(String tableName, Connection con) throws SQLException {
        List<String> keys = getTablePrimaryKeys(tableName, con);
        if (keys.size() > 0) {
            return keys.get(0);
        }
        return null;
    }

    @Override
    public List<String> getTablePrimaryKeys(String tableName, Connection con) throws SQLException {
        List<String> keys = new ArrayList<String>();
        DatabaseMetaData dbMeta = con.getMetaData();
        ResultSet rs = dbMeta.getPrimaryKeys(null, null, tableName);
        if (rs.next()) {
            keys.add(rs.getString("COLUMN_NAME"));
        }
        rs.close();
        return keys;
    }

    /**
     * 主键类型
     *
     * @param table
     * @param column 指定列名
     *
     * @return
     *
     * @throws SQLException
     */
    @Override
    public String getColumnType(CodeTable table, String column) throws SQLException {
        String colType = "";
        for (CodeColumn col : table.getColumns()) {
            if (col.getColumnName().equalsIgnoreCase(column)) {
                return col.getColumnType();
            }
        }
        return colType;
    }

    /**
     * 表注释
     *
     * @param tableName
     *
     * @return
     *
     * @throws SQLException
     */
    @Override
    public String getTableRemark(String tableName, Connection con) throws SQLException {
        String remark = "";
        String sql = "SELECT cast (ds.value as varchar(100)) comments FROM sys.extended_properties ds LEFT JOIN sysobjects tbs ON ds.major_id=tbs.id WHERE  ds.minor_id=0 and tbs.name=?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, tableName);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            remark = rs.getString("comments");
        }
        rs.close();
        ps.close();
        return remark;
    }

}
