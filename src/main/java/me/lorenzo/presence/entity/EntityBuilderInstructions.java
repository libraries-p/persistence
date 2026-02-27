package me.lorenzo.presence.entity;

import java.util.Map;

/*
This class explains how to assemble and disassemble data entities
*/
public abstract class EntityBuilderInstructions<T> {
    private final Class<T> presence;

    public EntityBuilderInstructions(Class<T> presence) {
        this.presence = presence;
    }

    public abstract Map<String, Object> disassemble(T entity);

    public abstract T assemble(Map<String, Object> instructions);

    public Class<T> asType() {
        return presence;
    }
}
