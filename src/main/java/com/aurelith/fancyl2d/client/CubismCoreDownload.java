package com.aurelith.fancyl2d.client;

import com.aurelith.fancyl2d.FancyL2D;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

final class CubismCoreDownload {
    private static final URI SOURCE = URI.create("https://cubism.live2d.com/sdk-web/cubismcore/live2dcubismcore.min.js");
    // The official file matching the Core used to verify the bundled Dragon_Balanced model.
    private static final String SHA256 = "25ae938cb4fe282ce189b357bcc97e603d1e1f7ec78bf04150d401c23cdc792f";
    private static final int MAX_BYTES = 1024 * 1024;
    private static final long RETRY_DELAY = Duration.ofMinutes(1).toNanos();
    private enum State { IDLE, DOWNLOADING, FAILED }

    private static volatile State state = State.IDLE;
    private static long failedAt;

    /** Called on the Minecraft client thread. A single download serves all elements. */
    static String unavailableMessage() {
        if (Files.isRegularFile(Live2DPage.CORE)) return null;
        switch (state) {
            case DOWNLOADING -> { return "fancyl2d.downloading_core"; }
            case FAILED -> {
                if (System.nanoTime() - failedAt < RETRY_DELAY) return "fancyl2d.core_download_failed";
            }
            case IDLE -> { }
        }
        state = State.DOWNLOADING;
        Thread.ofVirtual().name("FancyL2D Core downloader").start(CubismCoreDownload::download);
        return "fancyl2d.downloading_core";
    }

    private static void download() {
        try {
            install(Live2DPage.CORE);
            state = State.IDLE;
        } catch (IOException | NoSuchAlgorithmException | RuntimeException failure) {
            FancyL2D.LOGGER.warn("Could not download Cubism Core from {}", SOURCE, failure);
            failedAt = System.nanoTime();
            state = State.FAILED;
        }
    }

    private static void install(Path destination) throws IOException, NoSuchAlgorithmException {
        if (Files.isRegularFile(destination)) return;
        byte[] script = fetch();
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(script));
        if (!SHA256.equals(hash)) throw new IOException("The official Cubism Core differs from the verified version (SHA-256 " + hash + ")");

        Path folder = Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(folder, ".cubism-core-", ".tmp");
        try {
            Files.write(temporary, script);
            try {
                Files.move(temporary, destination);
                FancyL2D.LOGGER.info("Installed Cubism Core at {}", destination);
            } catch (FileAlreadyExistsException exists) {
                if (!Files.isRegularFile(destination)) throw exists;
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static byte[] fetch() throws IOException {
        var connection = (HttpURLConnection) SOURCE.toURL().openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(15_000);
        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) throw new IOException("Unexpected HTTP status " + status);
            try (var input = connection.getInputStream()) {
                byte[] script = input.readNBytes(MAX_BYTES + 1);
                if (script.length > MAX_BYTES) throw new IOException("Cubism Core response exceeds the size limit");
                return script;
            }
        } finally {
            connection.disconnect();
        }
    }

    private CubismCoreDownload() {}
}
