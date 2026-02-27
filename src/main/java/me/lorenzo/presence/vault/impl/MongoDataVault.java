package me.lorenzo.presence.vault.impl;

import com.mongodb.client.*;
import me.lorenzo.presence.entity.EntityBuilderInstructions;
import me.lorenzo.presence.entity.service.EBIService;
import me.lorenzo.presence.vault.DataVault;
import me.lorenzo.presence.vault.query.Query;
import me.lorenzo.presence.vault.record.DataRecord;
import me.lorenzo.presence.vault.record.simple.SimpleDataRecord;
import me.lorenzo.services.service.holder.Services;
import org.bson.Document;

import java.util.*;
import java.util.stream.Collectors;

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
        Document filter = queryToDocument(query);
        Document doc = coll.find(filter).first();
        return doc == null ? Optional.empty() : Optional.of(new SimpleDataRecord(doc));
    }

    @Override
    public List<DataRecord> find(String collection, Query query) {
        MongoCollection<Document> coll = database.getCollection(collection);
        Document filter = queryToDocument(query);
        List<DataRecord> results = new ArrayList<>();
        coll.find(filter).forEach(doc -> results.add(new SimpleDataRecord(doc)));
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
    public void insert(String collection, DataRecord record) {
        MongoCollection<Document> coll = database.getCollection(collection);
        coll.insertOne(new Document(record.asMap()));
    }

    @Override
    public void update(String collection, Query query, DataRecord updates) {
        MongoCollection<Document> coll = database.getCollection(collection);
        Document filter = queryToDocument(query);
        Document updateDoc = new Document();
        updates.asMap().forEach(updateDoc::append);
        coll.updateMany(filter, new Document("$set", updateDoc));
    }

    @Override
    public void delete(String collection, Query query) {
        MongoCollection<Document> coll = database.getCollection(collection);
        Document filter = queryToDocument(query);
        coll.deleteMany(filter);
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