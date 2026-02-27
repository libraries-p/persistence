package me.lorenzo.presence.vault;

import me.lorenzo.presence.vault.query.Query;
import me.lorenzo.presence.vault.record.DataRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DataVault {

    Optional<DataRecord> findOne(String collection, Query query);

    List<DataRecord> find(String collection, Query query);

    <T> Optional<T> findOne(
            String collection,
            Query query,
            Class<T> type
    );

    <T> List<T> find(
            String collection,
            Query query,
            Class<T> type
    );

    void insert(String collection, DataRecord record);

    void update(
            String collection,
            Query query,
            DataRecord updates
    );

    void delete(String collection, Query query);
}
