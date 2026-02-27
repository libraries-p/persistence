package me.lorenzo.presence.vault.record;

import java.util.Map;

public interface DataRecord {

    Object get(String field);

    <T> T get(String field, Class<T> type);

    void set(String field, Object value);

    Map<String, Object> asMap();
}