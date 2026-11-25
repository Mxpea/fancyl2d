package com.aurelith.fancyl2d.client;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Live2DConfigTest {
    @TempDir Path temporary;

    @Test void oldAndMalformedValuesUseDefaults() {
        var defaults = Live2DSettings.DEFAULT;
        assertEquals(0, Live2DSettings.number(null, defaults.sway(), 0, 1));
        assertEquals(0.65, Live2DSettings.number("NaN", defaults.follow(), 0, 1));
        assertEquals(60, Live2DSettings.frameRate(null, defaults.fps()));
        assertEquals(60, Live2DSettings.frameRate("abc", defaults.fps()));
    }

    @Test void limitsSwayAndFrameRate() {
        assertEquals(0, Live2DSettings.number("-1", 0, 0, 1));
        assertEquals(1, Live2DSettings.number("1.5", 0, 0, 1));
        assertEquals(15, Live2DSettings.frameRate("1", 60));
        assertEquals(120, Live2DSettings.frameRate(" 240 ", 60));
    }

    @Test void forwardsAllSettingsToThePage() {
        var json = new Live2DSettings("a.model3.json", 1.2, 0.4, false, 0.7, 90).pageConfig("model/a.model3.json");
        assertEquals("model/a.model3.json", json.get("model").getAsString());
        assertEquals(1.2, json.get("scale").getAsDouble());
        assertEquals(0.4, json.get("follow").getAsDouble());
        assertFalse(json.get("idle").getAsBoolean());
        assertEquals(0.7, json.get("sway").getAsDouble());
        assertEquals(90, json.get("fps").getAsInt());
    }

    @Test void resolvesContainedFilesAndRejectsOtherPaths() throws IOException {
        Path root = Files.createDirectory(temporary.resolve("models")).toRealPath();
        Path nested = Files.createDirectory(root.resolve("Dragon"));
        Path model = Files.createFile(nested.resolve("Blue Dragon.model3.json"));
        Path outside = Files.createFile(temporary.resolve("outside.json"));
        assertEquals(model.toRealPath(), ContainedFile.resolve(root, "Dragon/Blue Dragon.model3.json"));
        for (String invalid : new String[]{"../outside.json", outside.toString(), "C:outside.json", "Dragon\\Blue Dragon.model3.json", "Dragon", "missing.json"}) {
            assertThrows(IOException.class, () -> ContainedFile.resolve(root, invalid), invalid);
        }
    }
}
