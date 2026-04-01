package org.fentanylsolutions.blpatches.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.sigpipe.jbsdiff.Patch;

public final class BetweenlandsJarRuntimePatcher {

    private static final Logger LOG = LogManager.getLogger("blpatches-core");
    private static final String PATCH_METADATA_PATH = "blpatches/thebetweenlands-jar-patch.properties";
    private static final String PATCH_RESOURCE_PATH = "blpatches/thebetweenlands-jar.patch";
    private static final String BETWEENLANDS_MOD_ID = "thebetweenlands";
    private static final String BETWEENLANDS_COREMOD = "thebetweenlands.core.TheBetweenlandsLoadingPlugin";

    private BetweenlandsJarRuntimePatcher() {}

    public static void patchIfNecessary(Map<String, Object> data) {
        if (!Boolean.TRUE.equals(data.get("runtimeDeobfuscationEnabled"))) {
            LOG.debug("Skipping whole-jar The Betweenlands patching in the deobfuscated dev environment");
            return;
        }

        Properties metadata = loadMetadata();
        if (metadata.isEmpty()) {
            return;
        }

        Path gameDir = toGameDir(data.get("mcLocation"));
        if (gameDir == null) {
            LOG.warn("Could not determine the game directory for runtime The Betweenlands patching");
            return;
        }

        String officialJarName = metadata.getProperty("officialJarName");
        String originalSha256 = metadata.getProperty("originalSha256");
        String patchedSha256 = metadata.getProperty("patchedSha256");
        if (officialJarName == null || originalSha256 == null || patchedSha256 == null) {
            LOG.error("The bundled The Betweenlands patch metadata is missing required fields");
            return;
        }

        Path liveJar = locateBetweenlandsJar(gameDir, officialJarName);
        if (liveJar == null) {
            LOG.warn("Could not find {} under {}", officialJarName, gameDir.resolve("mods"));
            return;
        }

        try {
            Path backupJar = gameDir.resolve(".blpatches")
                .resolve("official")
                .resolve(officialJarName);
            byte[] liveBytes = Files.readAllBytes(liveJar);
            String liveHash = Hashing.sha256(liveBytes);
            if (patchedSha256.equals(liveHash)) {
                LOG.info("The Betweenlands is already patched at {}", liveJar);
                return;
            }

            byte[] originalBytes;
            if (originalSha256.equals(liveHash)) {
                originalBytes = liveBytes;
                ensureBackup(backupJar, liveBytes, originalSha256);
            } else if (Files.exists(backupJar)) {
                byte[] backupBytes = Files.readAllBytes(backupJar);
                String backupHash = Hashing.sha256(backupBytes);
                if (!originalSha256.equals(backupHash)) {
                    LOG.error(
                        "The stored official The Betweenlands backup {} has hash {} but {} is required",
                        backupJar,
                        backupHash,
                        originalSha256);
                    return;
                }
                originalBytes = backupBytes;
            } else {
                LOG.error(
                    "Cannot patch {} because its hash {} matches neither the expected official jar nor the patched jar",
                    liveJar,
                    liveHash);
                return;
            }

            byte[] patchBytes = readPatchBytes();
            byte[] patchedBytes = applyPatch(originalBytes, patchBytes);
            String actualPatchedHash = Hashing.sha256(patchedBytes);
            if (!patchedSha256.equals(actualPatchedHash)) {
                LOG.error(
                    "Patched The Betweenlands jar hash mismatch: expected {}, got {}",
                    patchedSha256,
                    actualPatchedHash);
                return;
            }

            writeAtomically(liveJar, patchedBytes);
            LOG.info("Patched The Betweenlands in place at {}", liveJar);
        } catch (Exception exception) {
            LOG.error("Failed to apply the whole-jar The Betweenlands patch", exception);
        }
    }

    private static Properties loadMetadata() {
        Properties properties = new Properties();
        try (InputStream input = BetweenlandsJarRuntimePatcher.class.getClassLoader()
            .getResourceAsStream(PATCH_METADATA_PATH)) {
            if (input == null) {
                LOG.warn("No bundled The Betweenlands jar patch metadata found at {}", PATCH_METADATA_PATH);
                return properties;
            }
            properties.load(input);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load bundled The Betweenlands jar patch metadata", exception);
        }
    }

    private static Path toGameDir(Object mcLocation) {
        if (mcLocation instanceof java.io.File) {
            return ((java.io.File) mcLocation).toPath();
        }
        return null;
    }

    private static Path locateBetweenlandsJar(Path gameDir, String officialJarName) {
        Path[] searchRoots = new Path[] { gameDir.resolve("mods"), gameDir.resolve("mods")
            .resolve("1.7.10") };
        for (Path root : searchRoots) {
            Path exactMatch = root.resolve(officialJarName);
            if (Files.exists(exactMatch)) {
                return exactMatch;
            }
        }

        for (Path root : searchRoots) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> stream = Files.list(root)) {
                Path fallback = stream.filter(Files::isRegularFile)
                    .filter(path -> isBetweenlandsJar(path, officialJarName))
                    .findFirst()
                    .orElse(null);
                if (fallback != null) {
                    return fallback;
                }
            } catch (IOException exception) {
                LOG.warn("Failed to scan {}", root, exception);
            }
        }
        return null;
    }

    private static boolean isBetweenlandsJar(Path jarPath, String officialJarName) {
        String fileName = jarPath.getFileName()
            .toString();
        if (!fileName.endsWith(".jar")) {
            return false;
        }
        if (fileName.equals(officialJarName) || fileName.startsWith(jarPrefix(officialJarName))) {
            return true;
        }

        try (ZipFile zipFile = new ZipFile(jarPath.toFile())) {
            return hasCoreModManifest(zipFile) || hasBetweenlandsMcmodInfo(zipFile);
        } catch (IOException exception) {
            LOG.debug("Failed to inspect {} while looking for The Betweenlands", jarPath, exception);
            return false;
        }
    }

    private static String jarPrefix(String jarName) {
        int dash = jarName.indexOf('-');
        int dot = jarName.indexOf('.');
        int end = dash >= 0 ? dash : dot >= 0 ? dot : jarName.length();
        return jarName.substring(0, end);
    }

    private static boolean hasCoreModManifest(ZipFile zipFile) throws IOException {
        ZipEntry manifestEntry = zipFile.getEntry("META-INF/MANIFEST.MF");
        if (manifestEntry == null) {
            return false;
        }

        try (InputStream input = zipFile.getInputStream(manifestEntry)) {
            String manifest = new String(readAllBytes(input), StandardCharsets.UTF_8);
            return manifest.contains("FMLCorePlugin: " + BETWEENLANDS_COREMOD);
        }
    }

    private static boolean hasBetweenlandsMcmodInfo(ZipFile zipFile) throws IOException {
        ZipEntry mcmodEntry = zipFile.getEntry("mcmod.info");
        if (mcmodEntry == null) {
            return false;
        }

        try (InputStream input = zipFile.getInputStream(mcmodEntry)) {
            String mcmodInfo = new String(readAllBytes(input), StandardCharsets.UTF_8);
            return mcmodInfo.contains("\"modid\": \"" + BETWEENLANDS_MOD_ID + "\"")
                || mcmodInfo.contains("\"modid\":\"" + BETWEENLANDS_MOD_ID + "\"");
        }
    }

    private static void ensureBackup(Path backupJar, byte[] liveBytes, String expectedOriginalHash) throws IOException {
        if (Files.exists(backupJar)) {
            String backupHash = Hashing.sha256(Files.readAllBytes(backupJar));
            if (expectedOriginalHash.equals(backupHash)) {
                return;
            }
        }
        Files.createDirectories(backupJar.getParent());
        Files.write(backupJar, liveBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static byte[] readPatchBytes() {
        try (InputStream input = BetweenlandsJarRuntimePatcher.class.getClassLoader()
            .getResourceAsStream(PATCH_RESOURCE_PATH)) {
            if (input == null) {
                throw new IllegalStateException(
                    "Missing bundled The Betweenlands jar patch resource " + PATCH_RESOURCE_PATH);
            }
            return readAllBytes(input);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Unable to read the bundled The Betweenlands jar patch resource",
                exception);
        }
    }

    private static byte[] applyPatch(byte[] originalBytes, byte[] patchBytes) throws IOException {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Patch.patch(originalBytes, patchBytes, output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IOException("Failed to apply the bundled The Betweenlands jar patch", exception);
        }
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static void writeAtomically(Path target, byte[] bytes) throws IOException {
        Path tempFile = target.resolveSibling(
            target.getFileName()
                .toString() + ".blpatches.tmp");
        try (OutputStream output = Files.newOutputStream(
            tempFile,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE)) {
            output.write(bytes);
        }
        try {
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ignored) {
            Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
