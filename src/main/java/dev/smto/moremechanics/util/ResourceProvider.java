package dev.smto.moremechanics.util;

import dev.smto.moremechanics.MoreMechanics;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class ResourceProvider {
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static final Path ROOT = FabricLoader.getInstance().getModContainer(MoreMechanics.MOD_ID).get().getRootPaths().getFirst().toAbsolutePath().normalize();

    public static String readFile(Path target) {
        try {
            return Files.readString(target, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    public static String readFile(String targetDir, String targetFileName) {
        try {
            var files = ResourceProvider.enumerateRootDirectory(targetDir, Filter.FILES_ONLY);
            for (Path file : files) {
                if (file.getFileName().toString().equals(targetFileName)) {
                    return ResourceProvider.readFile(file);
                }
            }
        } catch (Throwable ignored) {
            throw new RuntimeException(targetFileName + " could not be found/read in/from the JAR!");
        }
        return "";
    }

    public static List<Path> enumerateRootDirectory(String resourcePath, @Nullable Filter filter) throws Exception {
        if (MoreMechanics.DEV_MODE) {
            return ResourceProvider.enumerateDirectory(Path.of(Objects.requireNonNull(ResourceProvider.class.getClassLoader().getResource(resourcePath)).toURI()),filter);
        }
        return ResourceProvider.enumerateDirectory(ResourceProvider.ROOT.resolve(resourcePath),filter);
    }

    public static List<Path> enumerateDirectory(Path resourcePath, @Nullable Filter filter) {
        List<Path> out = new ArrayList<>();
        if (!Files.exists(resourcePath)) return out;
        try (Stream<Path> contentsStream = Files.list(resourcePath)) {
            var contents = contentsStream.toList();
            if (filter != null) {
                if (filter == Filter.FILES_ONLY) {
                    for (Path resource : contents) {
                        if (!Files.isDirectory(resource)) out.add(resource);
                    }
                } else if (filter == Filter.DIRECTORIES_ONLY) {
                    for (Path resource : contents) {
                        if (Files.isDirectory(resource)) out.add(resource);
                    }
                }
                return out;
            } else return contents;
        } catch (Throwable ignored) {}
        return out;
    }

    public enum Filter {
        FILES_ONLY,
        DIRECTORIES_ONLY
    }
}
