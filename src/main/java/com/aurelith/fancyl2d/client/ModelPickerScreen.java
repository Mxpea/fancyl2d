package com.aurelith.fancyl2d.client;

import com.aurelith.fancyl2d.FancyL2D;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

final class ModelPickerScreen extends Screen {
    private final Screen parent;
    private final Consumer<String> select;
    private final List<String> models;
    private final boolean scanFailed;
    private int page;
    private String query = "";
    private EditBox search;
    private Button[] rows;
    private Button previous;
    private Button next;
    private int pageCount;
    private int resultCount;

    ModelPickerScreen(Screen parent, Consumer<String> select) {
        super(Component.translatable("fancyl2d.choose_model"));
        this.parent = parent;
        this.select = select;
        List<String> found = List.of();
        boolean failed = false;
        try {
            found = findModels();
        } catch (IOException | UncheckedIOException ex) {
            FancyL2D.LOGGER.warn("Could not list Live2D models", ex);
            failed = true;
        }
        var available = new ArrayList<String>(found.size() + 1);
        available.add(ModelSource.DEFAULT_ID);
        available.addAll(found);
        models = List.copyOf(available);
        scanFailed = failed;
    }

    private static List<String> findModels() throws IOException {
        if (!Files.exists(Live2DPage.MODELS)) return List.of();
        Path root = Live2DPage.MODELS.toRealPath();
        try (var files = Files.walk(root)) {
            return files.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".model3.json"))
                .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                .map(path -> root.relativize(path).toString().replace('\\', '/'))
                .filter(path -> {
                    try {
                        ContainedFile.resolve(root, path);
                        return true;
                    } catch (IOException ex) {
                        return false;
                    }
                })
                .sorted(String.CASE_INSENSITIVE_ORDER.thenComparing(String::compareTo))
                .toList();
        }
    }

    @Override protected void init() {
        int content = Math.min(380, width - 20), left = (width - content) / 2;
        search = new EditBox(font, left, 31, content, 20, Component.translatable("fancyl2d.search_model"));
        search.setHint(Component.translatable("fancyl2d.search_model"));
        search.setMaxLength(256);
        search.setValue(query);
        addRenderableWidget(search);
        setInitialFocus(search);
        rows = new Button[Math.max(1, (height - 100) / 22)];
        for (int i = 0; i < rows.length; i++) {
            int index = i;
            rows[i] = addRenderableWidget(Button.builder(Component.empty(), ignored -> choose(index))
                .bounds(left, 55 + i * 22, content, 20).build());
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), ignored -> { page--; refresh(); })
            .bounds(width / 2 - 105, height - 30, 25, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), ignored -> { page++; refresh(); })
            .bounds(width / 2 - 24, height - 30, 25, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, ignored -> onClose())
            .bounds(width / 2 + 12, height - 30, 93, 20).build());
        search.setResponder(value -> { query = value; page = 0; refresh(); });
        refresh();
    }

    private String label(String path) {
        return ModelSource.DEFAULT_ID.equals(path) ? Component.translatable("fancyl2d.builtin_dragon").getString() : path;
    }

    private List<String> matches() {
        String normalized = query.strip().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? models : models.stream()
            .filter(path -> path.toLowerCase(Locale.ROOT).contains(normalized) || label(path).toLowerCase(Locale.ROOT).contains(normalized)).toList();
    }

    private void refresh() {
        var results = matches();
        resultCount = results.size();
        pageCount = Math.max(1, Math.ceilDiv(resultCount, rows.length));
        page = Math.clamp(page, 0, pageCount - 1);
        for (int i = 0; i < rows.length; i++) {
            int index = page * rows.length + i;
            rows[i].visible = index < resultCount;
            if (rows[i].visible) {
                String path = results.get(index);
                rows[i].setMessage(Component.literal(font.plainSubstrByWidth(label(path), rows[i].getWidth() - 12)));
                rows[i].setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(label(path))));
            }
        }
        previous.active = page > 0;
        next.active = page + 1 < pageCount;
    }

    private void choose(int index) {
        var results = matches();
        int position = page * rows.length + index;
        if (position < results.size()) {
            select.accept(results.get(position));
            onClose();
        }
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, 14, 0xffffffff);
        if (resultCount == 0) {
            String message = "fancyl2d.no_matches";
            graphics.centeredText(font, Component.translatable(message), width / 2, 65, 0xffdddddd);
        }
        if (scanFailed) graphics.centeredText(font, Component.translatable("fancyl2d.scan_failed"), width / 2, height - 44, 0xffdddddd);
        graphics.centeredText(font, (page + 1) + "/" + pageCount, width / 2 - 52, height - 24, 0xffffffff);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
}
