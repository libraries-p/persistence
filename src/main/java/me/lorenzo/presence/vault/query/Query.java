package me.lorenzo.presence.vault.query;

import java.util.HashMap;
import java.util.Map;

public final class Query {

    private final Map<String, Object> filters = new HashMap<>();

    public static Query where(String field, Object value) {
        Query q = new Query();
        q.filters.put(field, value);
        return q;
    }

    public Query and(String field, Object value) {
        filters.put(field, value);
        return this;
    }

    public Map<String, Object> filters() {
        return filters;
    }
}
