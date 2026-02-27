package me.lorenzo.presence.vault.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Query {

    private final Map<String, Object> filters = new HashMap<>();
    private int limit = -1;
    private int offset = -1;

    public static Query where(String field, Object value) {
        Query q = new Query();
        q.filters.put(field, value);
        return q;
    }

    public static Query all() {
        return new Query();
    }

    public Query and(String field, Object value) {
        filters.put(field, value);
        return this;
    }

    public Query limit(int limit) {
        this.limit = limit;
        return this;
    }

    public Query offset(int offset) {
        this.offset = offset;
        return this;
    }

    public Map<String, Object> filters() {
        return Collections.unmodifiableMap(filters);
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }
}
