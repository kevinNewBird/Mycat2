package io.mycat.util;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertInto;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import io.mycat.DataNode;
import io.mycat.MetaClusterCurrent;
import io.mycat.MetadataManager;
import io.mycat.TableHandler;
import io.mycat.calcite.executor.MycatPreparedStatementUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.sql.Statement.NO_GENERATED_KEYS;
import static java.sql.Statement.RETURN_GENERATED_KEYS;

/**
 * 真正要发给后端的sql语句
 * 用于把发送SQL过程中的变量数据内聚到一起， 不需要开发者从各个地方寻找数据。
 * @param <T>
 * @author wangzihaogithub 2020-12-29
 */
@Slf4j
@Getter
@ToString
@AllArgsConstructor
public class SQL<T extends SQLStatement> {
    private String parameterizedSql;
    private DataNode dataNode;
    private T statement;
    private List<Object> parameters;

    public String getTarget() {
        return dataNode.getTargetName();
    }

    public static <T extends SQLStatement> SQL<T> of(String parameterizedSql,
                                                     DataNode dataNode, T statement, List<Object> parameters){
        SQL sql;
        if(statement instanceof SQLUpdateStatement){
            sql = new UpdateSQL<>(parameterizedSql,dataNode,(SQLUpdateStatement)statement,parameters);
        }else {
            sql = new SQL<>(parameterizedSql,dataNode,statement,parameters);
        }
        return sql;
    }

    public UpdateResult executeUpdate(Connection connection) throws SQLException {
        return executeUpdate(connection,statement instanceof SQLInsertInto);
    }

    public UpdateResult executeUpdate(Connection connection,boolean autoGeneratedKeys) throws SQLException {
        List<Object> parameters = getParameters();
        if (!parameters.isEmpty()&&parameters.get(0) instanceof List){
            PreparedStatement preparedStatement = connection.prepareStatement(parameterizedSql, autoGeneratedKeys? RETURN_GENERATED_KEYS : NO_GENERATED_KEYS);
            for (Object parameter : parameters) {
                MycatPreparedStatementUtil.setParams(preparedStatement,(List) parameter);
                preparedStatement.addBatch();
            }
            int[] affectedRow = preparedStatement.executeBatch();
            Long lastInsertId = autoGeneratedKeys? getInSingleSqlLastInsertId(preparedStatement) : null;
            return new UpdateResult(Arrays.stream(affectedRow).sum(),lastInsertId);
        }else {
            PreparedStatement preparedStatement = connection.prepareStatement(parameterizedSql, autoGeneratedKeys? RETURN_GENERATED_KEYS : NO_GENERATED_KEYS);
            MycatPreparedStatementUtil.setParams(preparedStatement, this.parameters);
            int affectedRow = preparedStatement.executeUpdate();
            Long lastInsertId = autoGeneratedKeys? getInSingleSqlLastInsertId(preparedStatement) : null;
            return new UpdateResult(affectedRow,lastInsertId);
        }

    }

    /**
     * ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
     * 会生成多个值,其中第一个是真正的值
     *
     * @param preparedStatement
     * @return
     * @throws SQLException
     */
    public static Long getInSingleSqlLastInsertId(java.sql.Statement preparedStatement) throws SQLException {
        Long lastInsertId = null;
        ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
        if (generatedKeys != null) {
            if (generatedKeys.next()) {
                long generatedKeysLong = generatedKeys.getBigDecimal(1).longValue();
                if (log.isDebugEnabled()) {
                    log.debug("preparedStatement:{} insertId:{}", preparedStatement, generatedKeysLong);
                }
                lastInsertId = Math.max(generatedKeysLong,0);
            }
        }
        return lastInsertId;
    }


    @Getter
    @AllArgsConstructor
    public static class UpdateResult{
        private int affectedRow;
        private Long lastInsertId;
    }

    public TableHandler getTable(){
        TableHandler table = getMetadataManager().getTable(dataNode.getSchema(), dataNode.getTable());
        return table;
    }

    public MetadataManager getMetadataManager(){
        MetadataManager metadataManager = MetaClusterCurrent.wrapper(MetadataManager.class);
        return metadataManager;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SQL that = (SQL) o;
        return Objects.equals(that.getTarget(),getTarget())
                && Objects.equals(that.parameterizedSql,this.parameterizedSql);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.parameterizedSql,getTarget());
    }
}
