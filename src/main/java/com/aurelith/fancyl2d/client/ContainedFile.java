package com.aurelith.fancyl2d.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class ContainedFile {
    static Path resolve(Path root, String relative) throws IOException {
        // Reject Windows drive paths, alternate data streams and mixed URL/file separators.
        if (relative.isBlank() || relative.indexOf(':') >= 0 || relative.indexOf('\\') >= 0) throw new IOException("Invalid relative path");
        Path path = Path.of(relative);
        if (path.isAbsolute() || path.getRoot() != null) throw new IOException("Absolute path");
        Path resolved = root.resolve(path).normalize();
        if (!resolved.startsWith(root)) throw new IOException("Outside resource directory");
        Path real = resolved.toRealPath();
        if (!real.startsWith(root) || !Files.isRegularFile(real)) throw new IOException("Not a contained resource file");
        return real;
    }

    private ContainedFile() {}
}
