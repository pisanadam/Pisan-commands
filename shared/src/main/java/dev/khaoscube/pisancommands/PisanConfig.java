package dev.khaoscube.pisancommands;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class PisanConfig {
    private static final String SPEED_KEY = "minecart-max-speed-blocks-per-second";
    private static final double DEFAULT_SPEED = 8.0;

    private final Path path;
    private volatile double minecartSpeed = DEFAULT_SPEED;

    public PisanConfig() {
        this.path = FabricLoader.getInstance().getConfigDir().resolve("pisan_commands.properties");
    }

    public void load() {
        Properties properties = new Properties();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                properties.load(reader);
            } catch (IOException e) {
                PisanCommandsMod.LOGGER.warn("Pisan Commands config okunamadi: {}", path, e);
            }
        }

        String value = properties.getProperty(SPEED_KEY);
        if (value != null) {
            try {
                double parsed = Double.parseDouble(value);
                if (Double.isFinite(parsed) && parsed > 0.0 && parsed <= 1000.0) {
                    minecartSpeed = parsed;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        save();
    }

    public synchronized void setMinecartSpeed(double blocksPerSecond) {
        minecartSpeed = blocksPerSecond;
        save();
    }

    public double getMinecartSpeed() {
        return minecartSpeed;
    }

    private void save() {
        try {
            Files.createDirectories(path.getParent());
            Properties properties = new Properties();
            properties.setProperty(SPEED_KEY, Double.toString(minecartSpeed));
            try (Writer writer = Files.newBufferedWriter(path)) {
                properties.store(writer, "Pisan Commands Fabric configuration");
            }
        } catch (IOException e) {
            PisanCommandsMod.LOGGER.warn("Pisan Commands config yazilamadi: {}", path, e);
        }
    }
}
