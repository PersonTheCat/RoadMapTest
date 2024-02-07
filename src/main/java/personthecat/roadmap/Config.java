package personthecat.roadmap;

import personthecat.fastnoise.data.NoiseType;
import xjs.comments.CommentType;
import xjs.core.Json;
import xjs.core.JsonObject;
import xjs.core.JsonValue;
import xjs.exception.SyntaxException;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class Config {

    private final File file;
    private int chunkHeight = 64;
    private int chunkWidth = 64;
    private int grooveSize = 20;
    private int minY = -100;
    private int maxY = 96;
    private int resolution = 12;
    private int scrollCoolDown = 3;
    private int gridOpacity = 14;
    private float frequency = 0.0025F;
    private float grooveFrequency = 0.02F;
    private float surfaceScale = 0.6F;
    private float sideViewAngle = 0.8F;
    private float zoom = 1.25F;
    private boolean sideView = false;
    private boolean mountains = true;
    private NoiseType mapType = NoiseType.SIMPLEX;
    private NoiseType grooveType = NoiseType.PERLIN;
    private Color backgroundColor = Color.BLACK;
    private boolean hasErrors = false;
    private boolean missingFields = false;

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

    public int getScrollCoolDown() {
        return this.scrollCoolDown;
    }

    public int getGridOpacity() {
        return this.gridOpacity;
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

    public boolean isSideView() {
        return this.sideView;
    }

    public boolean isMountains() {
        return this.mountains;
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
        this.getInt(json, "chunkHeight", i -> i > 8 && i <= 64, "Must be 8 ~ 64")
            .ifPresent(i -> this.chunkHeight = i);
        this.getInt(json,"chunkWidth", i -> i > 8 && i <= 96, "Must be 8 ~ 96")
            .ifPresent(i -> this.chunkWidth = i);
        this.getInt(json, "grooveSize", i -> i >= 0 && i <= 128, "Must be 0 ~ 128")
            .ifPresent(i -> this.grooveSize = i);
        this.getInt(json, "minY", i -> i >= -128 && i <= 0, "Must be -128 ~ 0")
            .ifPresent(i -> this.minY = i);
        this.getInt(json, "maxY", i -> i > 0 && i <= 128, "Must be 1 ~ 128")
            .ifPresent(i -> this.maxY = i);
        this.getInt(json, "resolution", i -> i > 0 && i <= this.maxY, "Must be 1 ~ maxY")
            .ifPresent(i -> this.resolution = i);
        this.getInt(json, "scrollCoolDown", i -> i >= 0, "Must be >= 0")
            .ifPresent(i -> this.scrollCoolDown = i);
        this.getInt(json, "gridOpacity", i -> i >= 0, "Must be >= 0")
            .ifPresent(i -> this.gridOpacity = i);
        this.getFloat(json, "frequency", f -> f > 0, "Must be > 0")
            .ifPresent(f -> this.frequency = f);
        this.getFloat(json, "grooveFrequency", f -> f > 0, "Must be > 0")
            .ifPresent(f -> this.grooveFrequency = f);
        this.getFloat(json, "surfaceScale", f -> f >= 0 && f <= 1, "Must be 0 ~ 1")
            .ifPresent(f -> this.surfaceScale = f);
        this.getFloat(json, "sideViewAngle", f -> f >= 0, "Must be > 0")
            .ifPresent(f -> this.sideViewAngle = f);
        this.getFloat(json, "zoom", f -> f >= 0, "Must be > 0")
            .ifPresent(f -> this.zoom = f);
        this.getBoolean(json, "mountains").ifPresent(b -> this.mountains = b);
        this.getBoolean(json, "sideView").ifPresent(b -> this.sideView = b);
        this.getEnum(json, "mapType", NoiseType.class, NoiseType::from)
            .ifPresent(e -> this.mapType = e);
        this.getEnum(json, "grooveType", NoiseType.class, NoiseType::from)
            .ifPresent(e -> this.grooveType = e);
        this.getEnum(json, "backgroundColor", BackgroundColor.class, BackgroundColor::from)
            .ifPresent(c -> this.backgroundColor = c.get());
    }

    private Optional<Integer> getInt(
            final JsonObject json, final String key, final Predicate<Integer> filter, final String ifError) {
        return this.get(json, key, this.wrap(JsonValue::asInt, filter, ifError));
    }

    private Optional<Float> getFloat(
            final JsonObject json, final String key, final Predicate<Float> filter, final String ifError) {
        return this.get(json, key, this.wrap(JsonValue::asFloat, filter, ifError));
    }

    private Optional<Boolean> getBoolean(final JsonObject json, final String key) {
        return this.get(json, key, this.wrap(JsonValue::asBoolean, b -> true, "Unexpected error"));
    }

    private <E extends Enum<E>> Optional<E> getEnum(
            final JsonObject json, final String key, final Class<E> type, final Function<String, E> mapper) {
        return this.get(json, key, v -> {
            final String name = v.intoString();
            final E result = mapper.apply(name);
            if (result == null) {
                v.setComment(CommentType.EOL, "Must be one of " + Arrays.toString(type.getEnumConstants()));
            }
            return result;
        });
    }

    private <T> Optional<T> get(final JsonObject json, final String key, final Function<JsonValue, T> wrappedGetter) {
        if (!json.has(key)) {
            this.missingFields = true;
            return Optional.empty();
        }
        final Optional<T> value = json.getOptional(key, wrappedGetter);
        if (value.isEmpty()) {
            this.hasErrors = true;
        }
        return value;
    }

    private <T> Function<JsonValue, T> wrap(
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

    public void saveIfUpdated(final Tracker tracker) {
        if (this.sideView != tracker.isSideView()
                || this.mountains != tracker.isMountains()
                || this.zoom != tracker.getZoom()
                || this.sideViewAngle != tracker.getSideViewAngle()) {
            this.sideView = tracker.isSideView();
            this.mountains = tracker.isMountains();
            this.zoom = tracker.getZoom();
            this.sideViewAngle = tracker.getSideViewAngle();
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
            .add("scrollCoolDown", this.scrollCoolDown, "The scroll delay in ms.")
            .add("gridOpacity", this.gridOpacity, "The opacity of the main grid lines over the map.")
            .add("frequency", this.frequency, "Noise frequency for the main noise map.")
            .add("grooveFrequency", this.grooveFrequency, "Frequency for the groove noise.")
            .add("surfaceScale", this.surfaceScale, "The terrain scale when above sea level.")
            .add("sideViewAngle", this.sideViewAngle, "The ratio at which to drop closer pixels.")
            .add("zoom", this.zoom, "The zoom ratio in side view mode, e.g. > 1 to zoom in, < 1 to zoom out.")
            .add("mountains", this.mountains, "Whether to enable mountainous terrain scaling.")
            .add("sideView", this.sideView, "Whether to display the terrain in side view mode.")
            .add("mapType", this.mapType.format(), "The type of noise to generate for the primary map.")
            .add("grooveType", this.grooveType.format(), "The type of noise to generate for the grooves.")
            .add("backgroundColor", BackgroundColor.format(this.backgroundColor), "The color to display as the background in side view mode");
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
}
