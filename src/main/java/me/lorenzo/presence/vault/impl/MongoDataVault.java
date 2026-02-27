package me.lorenzo.presence.vault.impl;

import com.mongodb.client.*;
import com.mongodb.client.model.ReplaceOptions;
import me.lorenzo.presence.entity.EntityBuilderInstructions;
import me.lorenzo.presence.entity.service.EBIService;
import me.lorenzo.presence.vault.DataVault;
import me.lorenzo.presence.vault.query.Query;
import me.lorenzo.presence.vault.record.DataRecord;
import me.lorenzo.presence.vault.record.simple.SimpleDataRecord;
import me.lorenzo.services.service.holder.Services;
import org.bson.Document;

import java.util.*;

public class MongoDataVault implements DataVault {

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final EBIService ebiService;

    public MongoDataVault(String connectionString, String dbName) {
        this.mongoClient = MongoClients.create(connectionString);
        this.database = mongoClient.getDatabase(dbName);
        this.ebiService = Services.getOrThrow(EBIService.class);
    }

    @Override
    public Optional<DataRecord> findOne(String collection, Query query) {
        MongoCollection<Document> coll = database.getCollection(collection);
        Document doc = coll.find(queryToDocument(query)).first();
        return doc == null ? Optional.empty() : Optional.of(new SimpleDataRecord(doc));
    }

    @Override
    public List<DataRecord> find(String collection, Query query) {
        MongoCollection<Document> coll = database.getCollection(collection);
        FindIterable<Document> iterable = coll.find(queryToDocument(query));
        if (query.getLimit() > 0) iterable = iterable.limit(query.getLimit());
        if (query.getOffset() > 0) iterable = iterable.skip(query.getOffset());
        List<DataRecord> results = new ArrayList<>();
        iterable.forEach(doc -> results.add(new SimpleDataRecord(doc)));
        return results;
    }

    @Override
    public <T> Optional<T> findOne(String collection, Query query, Class<T> type) {
        EntityBuilderInstructions<T> builder = ebiService.getOrThrow(type);
        return findOne(collection, query).map(r -> builder.assemble(r.asMap()));
    }

    @Override
    public <T> List<T> find(String collection, Query query, Class<T> type) {
        EntityBuilderInstructions<T> builder = ebiService.getOrThrow(type);
        return find(collection, query).stream().map(r -> builder.assemble(r.asMap())).toList();
    }

    @Override
    public long count(String collection, Query query) {
        return database.getCollection(collection).countDocuments(queryToDocument(query));
    }

    @Override
    public void insert(String collection, DataRecord record) {
        database.getCollection(collection).insertOne(new Document(record.asMap()));
    }

    @Override
    public void insertMany(String collection, List<DataRecord> records) {
        if (records.isEmpty()) return;
        List<Document> docs = records.stream().map(r -> new Document(r.asMap())).toList();
        database.getCollection(collection).insertMany(docs);
    }

    @Override
    public void update(String collection, Query query, DataRecord updates) {
        database.getCollection(collection).updateMany(
                queryToDocument(query),
                new Document("$set", new Document(updates.asMap()))
        );
    }

    @Override
    public void upsert(String collection, Query query, DataRecord record) {
        database.getCollection(collection).replaceOne(
                queryToDocument(query),
                new Document(record.asMap()),
                new ReplaceOptions().upsert(true)
        );
    }

    @Override
    public void delete(String collection, Query query) {
        database.getCollection(collection).deleteMany(queryToDocument(query));
    }

    private Document queryToDocument(Query query) {
        Document doc = new Document();
        query.filters().forEach(doc::append);
        return doc;
    }

    public void close() {
        mongoClient.close();
    }
}
