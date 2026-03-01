package me.lorenzo.presence.vault.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.lorenzo.presence.entity.EntityBuilderInstructions;
import me.lorenzo.presence.entity.service.EBIService;
import me.lorenzo.presence.vault.DataVault;
import me.lorenzo.presence.vault.query.Query;
import me.lorenzo.presence.vault.record.DataRecord;
import me.lorenzo.presence.vault.record.simple.SimpleDataRecord;
import me.lorenzo.services.service.holder.Services;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class MySQLDataVault implements DataVault {

    private final HikariDataSource dataSource;

    public MySQLDataVault(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setPoolName("DataVaultPool");
        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public Optional<DataRecord> findOne(String collection, Query query) {
        List<String> filterKeys = new ArrayList<>(query.filters().keySet());
        String sql = buildSelectQuery(collection, filterKeys, query, true);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, filterKeys, query.filters(), 1);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(resultSetToRecord(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<DataRecord> find(String collection, Query query) {
        List<String> filterKeys = new ArrayList<>(query.filters().keySet());
        String sql = buildSelectQuery(collection, filterKeys, query, false);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, filterKeys, query.filters(), 1);
            try (ResultSet rs = ps.executeQuery()) {
                List<DataRecord> results = new ArrayList<>();
                while (rs.next()) results.add(resultSetToRecord(rs));
                return results;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> Optional<T> findOne(String collection, Query query, Class<T> type) {
        EntityBuilderInstructions<T> builder = Services.getOrThrow(EBIService.class).getOrThrow(type);
        return findOne(collection, query).map(r -> builder.assemble(r.asMap()));
    }

    @Override
    public <T> List<T> find(String collection, Query query, Class<T> type) {
        EntityBuilderInstructions<T> builder = Services.getOrThrow(EBIService.class).getOrThrow(type);
        return find(collection, query).stream()
                .map(r -> builder.assemble(r.asMap()))
                .toList();
    }

    @Override
    public long count(String collection, Query query) {
        validateIdentifier(collection);
        List<String> filterKeys = new ArrayList<>(query.filters().keySet());
        filterKeys.forEach(this::validateIdentifier);
        String sql = "SELECT COUNT(*) FROM `" + collection + "`";
        if (!filterKeys.isEmpty()) {
            String whereClause = filterKeys.stream()
                    .map(k -> "`" + k + "` = ?")
                    .collect(Collectors.joining(" AND "));
            sql += " WHERE " + whereClause;
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, filterKeys, query.filters(), 1);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void insert(String collection, DataRecord record) {
        insertRow(collection, record.asMap());
    }

    @Override
    public void insertMany(String collection, List<DataRecord> records) {
        if (records.isEmpty()) return;
        validateIdentifier(collection);
        List<String> columns = new ArrayList<>(records.get(0).asMap().keySet());
        columns.forEach(this::validateIdentifier);
        String colClause = columns.stream().map(c -> "`" + c + "`").collect(Collectors.joining(", "));
        String placeholders = columns.stream().map(c -> "?").collect(Collectors.joining(", "));
        String sql = "INSERT INTO `" + collection + "` (" + colClause + ") VALUES (" + placeholders + ")";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (DataRecord record : records) {
                Map<String, Object> row = record.asMap();
                int index = 1;
                for (String col : columns) ps.setObject(index++, row.get(col));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(String collection, Query query, DataRecord updates) {
        validateIdentifier(collection);
        List<String> updateKeys = new ArrayList<>(updates.asMap().keySet());
        List<String> filterKeys = new ArrayList<>(query.filters().keySet());
        updateKeys.forEach(this::validateIdentifier);
        filterKeys.forEach(this::validateIdentifier);
        if (updateKeys.isEmpty()) return;
        String setClause = updateKeys.stream().map(k -> "`" + k + "` = ?").collect(Collectors.joining(", "));
        String sql = "UPDATE `" + collection + "` SET " + setClause;
        if (!filterKeys.isEmpty()) {
            String whereClause = filterKeys.stream().map(k -> "`" + k + "` = ?").collect(Collectors.joining(" AND "));
            sql += " WHERE " + whereClause;
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = setParams(ps, updateKeys, updates.asMap(), 1);
            setParams(ps, filterKeys, query.filters(), index);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void upsert(String collection, Query query, DataRecord record) {
        Optional<DataRecord> existing = findOne(collection, query);
        if (existing.isPresent()) {
            update(collection, query, record);
        } else {
            insert(collection, record);
        }
    }

    @Override
    public void delete(String collection, Query query) {
        validateIdentifier(collection);
        List<String> filterKeys = new ArrayList<>(query.filters().keySet());
        filterKeys.forEach(this::validateIdentifier);
        String sql = "DELETE FROM `" + collection + "`";
        if (!filterKeys.isEmpty()) {
            String whereClause = filterKeys.stream().map(k -> "`" + k + "` = ?").collect(Collectors.joining(" AND "));
            sql += " WHERE " + whereClause;
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, filterKeys, query.filters(), 1);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void insertRow(String collection, Map<String, Object> row) {
        validateIdentifier(collection);
        List<String> columns = new ArrayList<>(row.keySet());
        columns.forEach(this::validateIdentifier);
        String colClause = columns.stream().map(c -> "`" + c + "`").collect(Collectors.joining(", "));
        String placeholders = columns.stream().map(c -> "?").collect(Collectors.joining(", "));
        String sql = "INSERT INTO `" + collection + "` (" + colClause + ") VALUES (" + placeholders + ")";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            for (String col : columns) ps.setObject(index++, row.get(col));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildSelectQuery(String collection, List<String> filterKeys, Query query, boolean single) {
        validateIdentifier(collection);
        filterKeys.forEach(this::validateIdentifier);
        String sql = "SELECT * FROM `" + collection + "`";
        if (!filterKeys.isEmpty()) {
            String whereClause = filterKeys.stream()
                    .map(k -> "`" + k + "` = ?")
                    .collect(Collectors.joining(" AND "));
            sql += " WHERE " + whereClause;
        }
        if (single) {
            sql += " LIMIT 1";
        } else {
            if (query.getLimit() > 0) sql += " LIMIT " + query.getLimit();
            if (query.getOffset() > 0) sql += " OFFSET " + query.getOffset();
        }
        return sql;
    }

    private int setParams(PreparedStatement ps, List<String> keys, Map<String, Object> values, int startIndex) throws SQLException {
        int index = startIndex;
        for (String key : keys) ps.setObject(index++, values.get(key));
        return index;
    }

    private DataRecord resultSetToRecord(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        Map<String, Object> map = new HashMap<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            map.put(meta.getColumnName(i), rs.getObject(i));
        }
        return new SimpleDataRecord(map);
    }

    private void validateIdentifier(String identifier) {
        if (!identifier.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            throw new IllegalArgumentException("Invalid SQL identifier: " + identifier);
        }
    }

    public void close() {
        dataSource.close();
    }
}
