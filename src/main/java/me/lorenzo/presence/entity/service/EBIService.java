package me.lorenzo.presence.entity.service;

import me.lorenzo.presence.entity.EntityBuilderInstructions;
import me.lorenzo.services.service.Service;

import java.util.HashMap;
import java.util.Map;

public class EBIService implements Service<EBIService> {
    private final Map<Class<?>, EntityBuilderInstructions<?>> builderInstructions = new HashMap<>();
    public void registerEBI(Class<?> clazz, EntityBuilderInstructions<?> instructions) {
        builderInstructions.put(clazz, instructions);
    }

    public <T> EntityBuilderInstructions<T> getOrThrow(Class<T> clazz) {
        EntityBuilderInstructions<T> instructions = (EntityBuilderInstructions<T>) builderInstructions.get(clazz);

        if (instructions == null) {
            throw new NullPointerException("No builder instructions registered for " + clazz);
        }

        return instructions;
    }

    @Override
    public Class<EBIService> asType() {
        return EBIService.class;
    }
}
