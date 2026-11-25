package com.aurelith.fancyl2d.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** An already resolved model and the scope in which its companion files may be read. */
record ModelSource(String fileName, Path directory) {
    static final String DEFAULT_ID = "builtin:dragon_balanced";
    private static final String DEFAULT_FILE = "Dragon_Balanced.model3.json";
    private static final String BUILTIN_ROOT = "/fancyl2d/models/dragon_balanced/";
    private static final String ASSET_NAME = "(?i)[a-z0-9_.-]+(?:/[a-z0-9_.-]+)*";

    static ModelSource resolve(Path localRoot, String id) throws IOException {
        if (DEFAULT_ID.equals(id)) return new ModelSource(DEFAULT_FILE, null);
        if (!id.toLowerCase(Locale.ROOT).endsWith(".model3.json")) {
            throw new IOException("Expected the built-in model ID or a relative .model3.json path");
        }
        Path model = ContainedFile.resolve(localRoot.toRealPath(), id);
        return new ModelSource(model.getFileName().toString(), model.getParent());
    }

    InputStream open(String asset) throws IOException {
        if (!asset.matches("(?i).+\\.(?:json|moc3|png|jpg|jpeg|webp|wav|ogg|mp3)")) return null;
        if (directory != null) return Files.newInputStream(ContainedFile.resolve(directory, asset));
        if (!asset.matches(ASSET_NAME)) return null;
        for (String part : asset.split("/")) if (part.equals(".") || part.equals("..")) return null;
        return ModelSource.class.getResourceAsStream(BUILTIN_ROOT + asset);
    }
}
