package personthecat.roadmap;

import personthecat.fastnoise.data.NoiseType;
import personthecat.roadmap.data.BackgroundColor;
import personthecat.roadmap.data.Tracker;
import personthecat.roadmap.gen.road.Road;
import xjs.comments.CommentType;
import xjs.core.Json;
import xjs.core.JsonObject;
import xjs.core.JsonValue;
import xjs.exception.SyntaxException;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Config {
  private final File file;
  private int chunkHeight = 64;
  private int chunkWidth = 64;
  private int grooveSize = 50;
  private int minY = -50;
  private int maxY = 96;
  private int resolution = 6;
  private int scrollAmount = 6;
  private int scrollCoolDown = 2;
  private int gridOpacity = 0;
  private int seed = 0;
  private int xOffset = 0;
  private int yOffset = 0;
  private int minRoadLength = Road.MAX_DISTANCE / 4;
  private int maxRoadLength = Road.MAX_DISTANCE;
  private int shorelineCutoff = 20;
  private int mountainCutoff = 40;
  private int maxBranches = 15;
  private int pregenThreadCount = 4;
  private int pregenRadius = 15;
  private float frequency = 0.00125F;
  private float grooveFrequency = 0.02F;
  private float surfaceScale = 0.5F;
  private float sideViewAngle = 0.85F;
  private float zoom = 1.25F;
  private float roadChance = 1.0F / 4000.0F;
  private float pregenSkew = 0.25F;
  private boolean sideView = false;
  private boolean mountains = true;
  private boolean enableRoads = true;
  private boolean persistRoads = true;
  private boolean generatePartial = true;
  private boolean pregenRoads = false;
  private boolean debugPregenShape = true;
  private boolean highlightRoadEndpoints = false;
  private NoiseType mapType = NoiseType.SIMPLEX;
  private NoiseType grooveType = NoiseType.CUBIC;
  private Color backgroundColor = Color.BLACK;
  private boolean hasErrors = false;
  private boolean missingFields = false;
  private boolean terrainFeaturesUpdated;

  public Config(final File file) {
    this.file = file;
    this.reloadFromDisk();
  }

  public int getChunkHeight() {
    return this.chunkHeight;
  }

  public int getChunkWidth() {
    return this.chunkWidth;
  }

  public int getGrooveSize() {
    return this.grooveSize;
  }

  public int getMinY() {
    return minY;
  }

  public int getMaxY() {
    return maxY;
  }

  public int getResolution() {
    return this.resolution;
  }

  public int getScrollAmount() {
    return this.scrollAmount;
  }

  public int getScrollCoolDown() {
    return this.scrollCoolDown;
  }

  public int getGridOpacity() {
    return this.gridOpacity;
  }

  public int getSeed() {
    return this.seed;
  }

  public int getXOffset() {
    return this.xOffset;
  }

  public int getYOffset() {
    return this.yOffset;
  }

  public int getMinRoadLength() {
    return this.minRoadLength;
  }

  public int getMaxRoadLength() {
    return this.maxRoadLength;
  }

  public int getShorelineCutoff() {
    return this.shorelineCutoff;
  }

  public int getMountainCutoff() {
    return this.mountainCutoff;
  }

  public int getMaxBranches() {
    return this.maxBranches;
  }

  public int getPregenThreadCount() {
    return this.pregenThreadCount;
  }

  public int getPregenRadius() {
    return this.pregenRadius;
  }

  public float getFrequency() {
    return this.frequency;
  }

  public float getGrooveFrequency() {
    return this.grooveFrequency;
  }

  public float getSurfaceScale() {
    return this.surfaceScale;
  }

  public float getSideViewAngle() {
    return this.sideViewAngle;
  }

  public float getZoom() {
    return this.zoom;
  }

  public float getRoadChance() {
    return this.roadChance;
  }

  public float getPregenSkew() {
    return this.pregenSkew;
  }

  public boolean isSideView() {
    return this.sideView;
  }

  public boolean isMountains() {
    return this.mountains;
  }

  public boolean isEnableRoads() {
    return this.enableRoads;
  }

  public boolean isPersistRoads() {
    return this.persistRoads;
  }

  public boolean isGeneratePartial() {
    return this.generatePartial;
  }

  public boolean isPregenRoads() {
    return this.pregenRoads;
  }

  public boolean isDebugPregenShape() {
    return this.debugPregenShape;
  }

  public boolean isHighlightRoadEndpoints() {
    return this.highlightRoadEndpoints;
  }

  public NoiseType getMapType() {
    return this.mapType;
  }

  public NoiseType getGrooveType() {
    return this.grooveType;
  }

  public Color getBackgroundColor() {
    return this.backgroundColor;
  }

  public void reloadFromDisk() {
    try {
      if (!this.file.exists()) {
        this.save();
        System.out.println("Config not found. A new file was generated.");
        return;
      }
      final JsonObject json = Json.parse(this.file).asObject();
      this.deserialize(json);
      if (this.hasErrors) {
        this.save(json);
        this.hasErrors = false;
        System.out.println("Config loaded with errors. Check the comments for details.");
      } else if (this.missingFields) {
        this.save(json.setDefaults(this.toJson()));
        this.missingFields = false;
        System.out.println("Config loaded with missing values. These fields regenerated.");
      } else {
        System.out.println("Config loaded successfully!");
      }
    } catch (final IOException | SyntaxException | UnsupportedOperationException e) {
      e.printStackTrace();
      System.err.println("Error loading config: " + e.getMessage());
    }
  }

  private void deserialize(final JsonObject json) {
    this.terrainFeaturesUpdated = false;
    this.getInt(json, "chunkHeight")
        .filter(i -> i >= 8 && i <= 64)
        .error("Must be 8 ~ 64")
        .set(i -> this.chunkHeight = i);
    this.getInt(json,"chunkWidth")
        .filter(i -> i >= 8 && i <= 96)
        .error("Must be 8 ~ 96")
        .set(i -> this.chunkWidth = i);
    this.getInt(json, "grooveSize")
        .changesTerrainFeatures()
        .filter(i -> i >= 0 && i <= 128)
        .error("Must be 0 ~ 128")
        .get(() -> this.grooveSize)
        .set(i -> this.grooveSize = i);
    this.getInt(json, "minY")
        .changesTerrainFeatures()
        .filter(i -> i >= -128 && i <= 0)
        .error("Must be -128 ~ 0")
        .get(() -> this.minY)
        .set(i -> this.minY = i);
    this.getInt(json, "maxY")
        .changesTerrainFeatures()
        .filter(i -> i > 0 && i <= 128 && i >= this.minY)
        .error("Must be 1 ~ 128 && >= min")
        .get(() -> this.maxY)
        .set(i -> this.maxY = i);
    this.getInt(json, "resolution")
        .filter(i -> i > 0 && i <= this.maxY)
        .error("Must be 1 ~ maxY")
        .set(i -> this.resolution = i);
    this.getInt(json, "scrollAmount")
        .filter(i -> i > 0)
        .error("Must be > 0")
        .set(i -> this.scrollAmount = i);
    this.getInt(json, "scrollCoolDown")
        .filter(i -> i >= 0)
        .error("Must be >= 0")
        .set(i -> this.scrollCoolDown = i);
    this.getInt(json, "gridOpacity")
        .filter(i -> i >= 0)
        .error("Must be >= 0")
        .set(i -> this.gridOpacity = i);
    this.getInt(json, "seed").set(i -> this.seed = i);
    this.getInt(json, "xOffset").set(i -> this.xOffset = i);
    this.getInt(json, "yOffset").set(i -> this.yOffset = i);
    this.getInt(json, "minRoadLength")
        .changesTerrainFeatures()
        .filter(i -> i >= 0 && i <= Road.MAX_DISTANCE)
        .error("Must be 0 ~ " + Road.MAX_DISTANCE)
        .get(() -> this.minRoadLength)
        .set(i -> this.minRoadLength = i);
    this.getInt(json, "maxRoadLength")
        .changesTerrainFeatures()
        .filter(i -> i >= 0 && i <= Road.MAX_DISTANCE && i >= this.minRoadLength)
        .error("Must be 0 ~ " + Road.MAX_DISTANCE + " && >= min")
        .get(() -> this.maxRoadLength)
        .set(i -> this.maxRoadLength = i);
    this.getInt(json, "shorelineCutoff")
        .changesTerrainFeatures()
        .filter(i -> i >= this.minY && i <= this.maxY)
        .error("Must be in height bounds")
        .get(() -> this.shorelineCutoff)
        .set(i -> this.shorelineCutoff = i);
    this.getInt(json, "mountainCutoff")
        .changesTerrainFeatures()
        .filter(i -> i >= this.minY && i <= this.maxY)
        .error("Must be in height bounds")
        .get(() -> this.mountainCutoff)
        .set(i -> this.mountainCutoff = i);
    this.getInt(json, "maxBranches")
        .changesTerrainFeatures()
        .filter(i -> i >= 0 && i <= 64)
        .error("Must be 0 ~ 64")
        .get(() -> this.maxBranches)
        .set(i -> this.maxBranches = i);
    this.getInt(json, "pregenThreadCount")
        .filter(i -> i > 0 && i <= 8)
        .error("Must be 1 ~ 8")
        .set(i -> this.pregenThreadCount = i);
    this.getInt(json, "pregenRadius")
        .filter(i -> i > 0 && i < 128)
        .error("Must be 1 ~ 128")
        .set(i -> this.pregenRadius = i);
    this.getFloat(json, "frequency")
        .changesTerrainFeatures()
        .filter(f -> f > 0)
        .error("Must be > 0")
        .get(() -> this.frequency)
        .set(f -> this.frequency = f);
    this.getFloat(json, "grooveFrequency")
        .changesTerrainFeatures()
        .filter(f -> f > 0)
        .error("Must be positive")
        .get(() -> this.grooveFrequency)
        .set(f -> this.grooveFrequency = f);
    this.getFloat(json, "surfaceScale")
        .changesTerrainFeatures()
        .filter(f -> f >= 0 && f <= 1)
        .error("Must be 0 ~ 1")
        .get(() -> this.surfaceScale)
        .set(f -> this.surfaceScale = f);
    this.getFloat(json, "sideViewAngle")
        .filter(f -> f >= 0)
        .error("Must be positive")
        .set(f -> this.sideViewAngle = f);
    this.getFloat(json, "zoom")
        .filter(f -> f >= 0)
        .error("Must be positive")
        .set(f -> this.zoom = f);
    this.getFloat(json, "roadChance")
        .changesTerrainFeatures()
        .filter(f -> f >= 0 && f <= 1)
        .error("Must be 0 ~ 1")
        .get(() -> this.roadChance)
        .set(f -> this.roadChance = f);
    this.getFloat(json, "pregenSkew")
        .filter(f -> f >= 0 && f <= 1)
        .error("Must be 0 - 1")
        .set(f -> this.pregenSkew = f);
    this.getBoolean(json, "mountains")
        .changesTerrainFeatures()
        .get(() -> this.mountains)
        .set(b -> this.mountains = b);
    this.getBoolean(json, "sideView").set(b -> this.sideView = b);
    this.getBoolean(json, "enableRoads").set(b -> this.enableRoads = b);
    this.getBoolean(json, "persistRoads").set(b -> this.persistRoads = b);
    this.getBoolean(json, "generatePartial").set(b -> this.generatePartial = b);
    this.getBoolean(json, "pregenRoads").set(b -> this.pregenRoads = b);
    this.getBoolean(json, "debugPregenShape").set(b -> this.debugPregenShape = b);
    this.getBoolean(json, "highlightRoadEndpoints").set(b -> this.highlightRoadEndpoints = b);
    this.getEnum(json, "mapType", NoiseType.class, NoiseType::from)
        .changesTerrainFeatures()
        .get(() -> this.mapType)
        .set(e -> this.mapType = e);
    this.getEnum(json, "grooveType", NoiseType.class, NoiseType::from)
        .changesTerrainFeatures()
        .get(() -> this.grooveType)
        .set(e -> this.grooveType = e);
    this.getEnum(json, "backgroundColor", BackgroundColor.class, BackgroundColor::from)
        .set(c -> this.backgroundColor = c.get());
  }

  private ConfigValue<Integer> getInt(final JsonObject json, final String key) {
    return new ConfigValue<>(json, key, JsonValue::asInt);
  }

  private ConfigValue<Float> getFloat(final JsonObject json, final String key) {
    return new ConfigValue<>(json, key, JsonValue::asFloat);
  }

  private ConfigValue<Boolean> getBoolean(final JsonObject json, final String key) {
    return new ConfigValue<>(json, key, JsonValue::asBoolean);
  }

  private <E extends Enum<E>> ConfigValue<E> getEnum(
      final JsonObject json, final String key, final Class<E> type, final Function<String, E> mapper) {
    return new ConfigValue<>(json, key, v -> {
      final String name = v.intoString();
      final E result = mapper.apply(name);
      if (result == null) {
        v.setComment(CommentType.EOL, "Must be one of " + Arrays.toString(type.getEnumConstants()));
      }
      return result;
    });
  }

  public boolean terrainFeaturesUpdated() {
    return this.terrainFeaturesUpdated;
  }

  public void saveIfUpdated(final Tracker tracker) {
    if (this.seed != tracker.getSeed()
        || this.xOffset != tracker.getXOffset()
        || this.yOffset != tracker.getYOffset()
        || this.sideView != tracker.isSideView()
        || this.mountains != tracker.isMountains()
        || this.enableRoads != tracker.isEnableRoads()
        || this.zoom != tracker.getZoom()
        || this.sideViewAngle != tracker.getSideViewAngle()
        || this.frequency != tracker.getFrequency()
        || this.grooveFrequency != tracker.getGrooveFrequency()) {
      this.seed = tracker.getSeed();
      this.xOffset = tracker.getXOffset();
      this.yOffset = tracker.getYOffset();
      this.sideView = tracker.isSideView();
      this.mountains = tracker.isMountains();
      this.enableRoads = tracker.isEnableRoads();
      this.zoom = tracker.getZoom();
      this.sideViewAngle = tracker.getSideViewAngle();
      this.frequency = tracker.getFrequency();
      this.grooveFrequency = tracker.getGrooveFrequency();
      this.save();
    }
  }

  public void save() {
    this.save(this.toJson());
  }

  private JsonObject toJson() {
    return Json.object()
        .add("chunkHeight", this.chunkHeight, "The number of chunks to display vertically.")
        .add("chunkWidth", this.chunkWidth, "This number of chunks to display horizontally.")
        .add("grooveSize", this.grooveSize, "The depth of the added heightmap overlay.")
        .add("minY", this.minY, "The minimum y-coordinate to generate.")
        .add("maxY", this.maxY, "The maximum y-coordinate to generate.")
        .add("resolution", this.resolution, "The number of y-levels to render as the same color.")
        .add("scrollAmount", this.scrollAmount, "How many chunks to pan the camera while holding shift.")
        .add("scrollCoolDown", this.scrollCoolDown, "The scroll delay in ms.")
        .add("gridOpacity", this.gridOpacity, "The opacity of the main grid lines over the map.")
        .add("seed", this.seed, "The seed to use for the generated terrain.")
        .add("xOffset", this.xOffset, "The number of blocks to offset the generator on the x-axis.")
        .add("yOffset", this.yOffset, "The number of blocks to offset the generator on the y-axis.")
        .add("minRoadLength", this.minRoadLength, "The minimum length any road can be.")
        .add("maxRoadLength", this.maxRoadLength, "The maximum length any road can be.")
        .add("shorelineCutoff", this.shorelineCutoff, "The minimum height at which to avoid shorelines.")
        .add("mountainCutoff", this.mountainCutoff, "The minimum height at which to avoid mountains.")
        .add("maxBranches", this.maxBranches, "The max number of road branches at any level.")
        .add("pregenThreadCount", this.pregenThreadCount, "The number of threads on which the pre-generator can run.")
        .add("pregenRadius", this.pregenRadius, "The radius of road regions to pre-generate (d = r * 2 + 1)")
        .add("frequency", this.frequency, "Noise frequency for the main noise map.")
        .add("grooveFrequency", this.grooveFrequency, "Frequency for the groove noise.")
        .add("surfaceScale", this.surfaceScale, "The terrain scale when above sea level.")
        .add("sideViewAngle", this.sideViewAngle, "The ratio at which to drop closer pixels.")
        .add("zoom", this.zoom, "The zoom ratio in side view mode, e.g. > 1 to zoom in, < 1 to zoom out.")
        .add("roadChance", this.roadChance, "The chance of a road origin spawning in any given chunk.")
        .add("pregenSkew", this.pregenSkew, "An optimization which controls the ratio of regions pre-generated diagonally.")
        .add("mountains", this.mountains, "Whether to enable mountainous terrain scaling.")
        .add("sideView", this.sideView, "Whether to display the terrain in side view mode.")
        .add("enableRoads", this.enableRoads, "Whether to generate and display roads on the map.")
        .add("persistRoads", this.persistRoads, "Whether to save roads to the disk as they generate.")
        .add("generatePartial", this.generatePartial, "Whether to persist road data into un-generated regions to avoid redundancy.")
        .add("pregenRoads", this.pregenRoads, "Whether to pre-generate road regions surrounding the current offset on startup.")
        .add("debugPregenShape", this.debugPregenShape, "Whether to log a debug image of the shape of the regions generated.")
        .add("highlightRoadEndpoints", this.highlightRoadEndpoints, "Debug option to clearly show where road endpoints are.")
        .add("mapType", this.mapType.format(), "The type of noise to generate for the primary map.")
        .add("grooveType", this.grooveType.format(), "The type of noise to generate for the grooves.")
        .add("backgroundColor", BackgroundColor.format(this.backgroundColor), "The color to display as the background in side view mode.");
  }

  private void save(final JsonObject json) {
    try {
      json.write(this.file);
      System.out.println("Config updated successfully!");
    } catch (final IOException e) {
      e.printStackTrace();
      System.err.println("Error saving config: " + e.getMessage());
    }
  }

  private class ConfigValue<T> {
    private final JsonObject json;
    private final String key;
    private final Function<JsonValue, T> mapper;
    private Predicate<T> filter = t -> true;
    private Supplier<T> getter;
    private String message = "Unexpected error occurred";
    private boolean changesTerrainFeatures;

    ConfigValue(final JsonObject json, final String key, final Function<JsonValue, T> mapper) {
      this.json = json;
      this.key = key;
      this.mapper = mapper;
    }

    ConfigValue<T> filter(final Predicate<T> filter) {
      this.filter = filter;
      return this;
    }

    ConfigValue<T> error(final String message) {
      this.message = message;
      return this;
    }

    ConfigValue<T> changesTerrainFeatures() {
      this.changesTerrainFeatures = true;
      return this;
    }

    ConfigValue<T> get(final Supplier<T> getter) {
      this.getter = getter;
      return this;
    }

    void set(final Consumer<T> c) {
      if (!this.json.has(this.key)) {
        Config.this.missingFields = true;
        return;
      }
      final Optional<T> value =
          this.json.getOptional(this.key, wrap(this.mapper, this.filter, this.message));
      if (value.isEmpty()) {
        Config.this.hasErrors = true;
      } else if (this.changesTerrainFeatures) {
        Objects.requireNonNull(this.getter, "getter");
        final T current = this.getter.get();
        if (!value.get().equals(current)) {
          Config.this.terrainFeaturesUpdated = true;
        }
      }
      value.ifPresent(c);
    }

    static <T> Function<JsonValue, T> wrap(
        final Function<JsonValue, T> getter, final Predicate<T> filter, final String ifError) {
      return value -> {
        final T t = getter.apply(value);
        if (!filter.test(t)) {
          value.setComment(CommentType.EOL, ifError);
          throw new UnsupportedOperationException();
        }
        return t;
      };
    }
  }
}
