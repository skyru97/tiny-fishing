package com.tinyfishing.data;

import org.bson.BsonArray;
import org.bson.BsonDocument;

public final class JsonLoader {
    public BsonArray readArray(String json) {
        return BsonArray.parse(json);
    }

    public BsonDocument readDocument(String json) {
        return BsonDocument.parse(json);
    }
}
