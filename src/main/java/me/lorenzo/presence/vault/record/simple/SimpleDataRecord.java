package me.lorenzo.presence.vault.record.simple;

import me.lorenzo.presence.vault.record.DataRecord;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SimpleDataRecord implements DataRecord {

    private final Map<String, Object> data;

    public SimpleDataRecord() {
        this.data = new HashMap<>();
    }

    public SimpleDataRecord(Map<String, Object> data) {
        this.data = new HashMap<>(data);
    }

    @Override
    public Object get(String field) {
        return data.get(field);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String field, Class<T> type) {
        Object value = data.get(field);
        if (value == null) return null;
        if (type.isInstance(value)) return type.cast(value);

        if (type == Integer.class && value instanceof Number n) return type.cast(n.intValue());
        if (type == Long.class && value instanceof Number n) return type.cast(n.longValue());
        if (type == Double.class && value instanceof Number n) return type.cast(n.doubleValue());
        if (type == String.class) return type.cast(value.toString());
        if (type == Boolean.class && value instanceof Boolean b) return type.cast(b);

        throw new IllegalStateException("Cannot convert field '" + field + "' from " +
                value.getClass().getName() + " to " + type.getName());
    }

    @Override
    public void set(String field, Object value) {
        data.put(field, value);
    }

    @Override
    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(data);
    }
}