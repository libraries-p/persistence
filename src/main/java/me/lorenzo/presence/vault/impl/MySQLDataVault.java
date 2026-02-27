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
    private final EBIService ebiService;

    public MySQLDataVault(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setPoolName("DataVaultPool");
        this.dataSource = new HikariDataSource(config);
        this.ebiService = Services.getOrThrow(EBIService.class);
    }

    @Override
    public Optional<DataRecord> findOne(String collection, Query query) {
        String sql = buildSelectQuery(collection, query, true);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setQueryParameters(ps, query);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(resultSetToRecord(rs));
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<DataRecord> find(String collection, Query query) {
        String sql = buildSelectQuery(collection, query, false);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setQueryParameters(ps, query);
            ResultSet rs = ps.executeQuery();
            List<DataRecord> results = new ArrayList<>();
            while (rs.next()) results.add(resultSetToRecord(rs));
            return results;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> Optional<T> findOne(String collection, Query query, Class<T> type) {
        EntityBuilderInstructions<T> builder = ebiService.getOrThrow(type);
        Optional<DataRecord> record = findOne(collection, query);
        return record.map(r -> builder.assemble(r.asMap()));
    }

    @Override
    public <T> List<T> find(String collection, Query query, Class<T> type) {
        EntityBuilderInstructions<T> builder = ebiService.getOrThrow(type);
        return find(collection, query).stream()
                .map(r -> builder.assemble(r.asMap()))
                .toList();
    }

    @Override
    public void insert(String collection, DataRecord record) {
        insertRow(collection, record.asMap());
    }

    @Override
    public void update(String collection, Query query, DataRecord updates) {
        String setClause = updates.asMap().keySet().stream().map(k -> k + " = ?").collect(Collectors.joining(", "));
        String whereClause = query.filters().keySet().stream().map(k -> k + " = ?").collect(Collectors.joining(" AND "));
        String sql = "UPDATE " + collection + " SET " + setClause + " WHERE " + whereClause;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            for (String key : updates.asMap().keySet()) ps.setObject(index++, updates.asMap().get(key));
            for (String key : query.filters().keySet()) ps.setObject(index++, query.filters().get(key));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(String collection, Query query) {
        String whereClause = query.filters().keySet().stream().map(k -> k + " = ?").collect(Collectors.joining(" AND "));
        String sql = "DELETE FROM " + collection + " WHERE " + whereClause;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setQueryParameters(ps, query);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void insertRow(String collection, Map<String, Object> row) {
        String columns = String.join(", ", row.keySet());
        String placeholders = row.keySet().stream().map(k -> "?").collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + collection + " (" + columns + ") VALUES (" + placeholders + ")";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            for (String key : row.keySet()) ps.setObject(index++, row.get(key));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildSelectQuery(String collection, Query query, boolean single) {
        String whereClause = query.filters().keySet().stream().map(k -> k + " = ?").collect(Collectors.joining(" AND "));
        String sql = "SELECT * FROM " + collection;
        if (!query.filters().isEmpty()) sql += " WHERE " + whereClause;
        if (single) sql += " LIMIT 1";
        return sql;
    }

    private void setQueryParameters(PreparedStatement ps, Query query) throws SQLException {
        int index = 1;
        for (String key : query.filters().keySet()) ps.setObject(index++, query.filters().get(key));
    }

    private DataRecord resultSetToRecord(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        Map<String, Object> map = new HashMap<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            map.put(meta.getColumnName(i), rs.getObject(i));
        }
        return new SimpleDataRecord(map);
    }

    public void close() {
        dataSource.close();
    }
}