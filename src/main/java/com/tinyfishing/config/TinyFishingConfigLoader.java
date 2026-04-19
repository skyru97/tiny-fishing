package com.tinyfishing.config;

import com.tinyfishing.data.JsonLoader;
import com.tinyfishing.data.ResourceTextLoader;
import com.tinyfishing.item.CodexEntryDefinition;
import com.tinyfishing.item.FishDefinition;
import com.tinyfishing.item.FishingRegionDefinition;
import com.tinyfishing.item.RodDefinition;
import com.tinyfishing.item.WeightedFishingEntry;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;

public final class TinyFishingConfigLoader {
    private static final String BASE = "tinyfishing/fishing/";

    private final HytaleLogger logger;
    private final ResourceTextLoader resourceTextLoader = new ResourceTextLoader();
    private final JsonLoader jsonLoader = new JsonLoader();
    private final TinyFishingConfigValidator configValidator = new TinyFishingConfigValidator();

    public TinyFishingConfigLoader(HytaleLogger logger) {
        this.logger = logger;
    }

    public TinyFishingConfig loadBundled() {
        TinyFishingConfig config = new TinyFishingConfig(
            parseRods(readArray("rods.json")),
            parseFish(readArray("fish.json")),
            parseCodex(readArray("codex.json")),
            parseRegions(readArray("fishing-regions.json")),
            readStringList(readArray("prize-items.json"))
        );
        configValidator.validate(config);
        logger.at(Level.INFO).log(
            "Loaded Tiny Fishing config: %d rods, %d fish, %d regions",
            config.rods().size(),
            config.fishDefinitions().size(),
            config.fishingRegions().size()
        );
        return config;
    }

    private BsonArray readArray(String fileName) {
        return jsonLoader.readArray(resourceTextLoader.loadRequired(BASE + fileName));
    }

    private List<RodDefinition> parseRods(BsonArray bsonArray) {
        List<RodDefinition> rods = new ArrayList<>();
        for (BsonValue value : bsonArray) {
            BsonDocument document = value.asDocument();
            rods.add(new RodDefinition(
                document.getString("id").getValue(),
                document.getString("displayName").getValue(),
                document.getString("itemId").getValue(),
                document.getDouble("castRange").getValue(),
                document.getDouble("biteWindowSeconds").getValue(),
                document.getDouble("minBiteDelaySeconds").getValue(),
                document.getDouble("maxBiteDelaySeconds").getValue(),
                readStringList(document.getArray("tags"))
            ));
        }
        return rods;
    }

    private List<FishDefinition> parseFish(BsonArray bsonArray) {
        List<FishDefinition> fishDefinitions = new ArrayList<>();
        for (BsonValue value : bsonArray) {
            BsonDocument document = value.asDocument();
            fishDefinitions.add(new FishDefinition(
                document.getString("id").getValue(),
                document.getString("itemId").getValue(),
                document.getString("displayName").getValue(),
                document.getString("rarity").getValue(),
                document.getInt32("xp").getValue(),
                document.getString("codexEntryId").getValue()
            ));
        }
        return fishDefinitions;
    }

    private List<CodexEntryDefinition> parseCodex(BsonArray bsonArray) {
        List<CodexEntryDefinition> entries = new ArrayList<>();
        for (BsonValue value : bsonArray) {
            BsonDocument document = value.asDocument();
            entries.add(new CodexEntryDefinition(
                document.getString("id").getValue(),
                document.getString("displayName").getValue(),
                document.getString("iconPath").getValue(),
                document.getString("hint").getValue()
            ));
        }
        return entries;
    }

    private List<FishingRegionDefinition> parseRegions(BsonArray bsonArray) {
        List<FishingRegionDefinition> entries = new ArrayList<>();
        for (BsonValue value : bsonArray) {
            BsonDocument document = value.asDocument();
            entries.add(new FishingRegionDefinition(
                document.getString("regionId").getValue(),
                document.getString("displayName").getValue(),
                readStringList(document.getArray("environmentIds")),
                parseWeightedEntries(document.getArray("fishEntries")),
                parseWeightedEntries(document.getArray("trashEntries")),
                parseWeightedEntries(document.getArray("prizeEntries"))
            ));
        }
        return entries;
    }

    private List<WeightedFishingEntry> parseWeightedEntries(BsonArray bsonArray) {
        List<WeightedFishingEntry> entries = new ArrayList<>();
        for (BsonValue value : bsonArray) {
            BsonDocument document = value.asDocument();
            entries.add(new WeightedFishingEntry(
                document.containsKey("targetId") ? document.getString("targetId").getValue() : null,
                document.containsKey("itemId") ? document.getString("itemId").getValue() : null,
                document.getInt32("weight").getValue()
            ));
        }
        return entries;
    }

    private List<String> readStringList(BsonArray bsonArray) {
        List<String> values = new ArrayList<>();
        for (BsonValue value : bsonArray) {
            values.add(value.asString().getValue());
        }
        return values;
    }

}
