package com.aurelith.fancyl2d.client;

import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import java.util.function.BiConsumer;
import net.minecraft.network.chat.Component;

final class Live2DEditorElement extends AbstractEditorElement<Live2DEditorElement, Live2DElement> {
    Live2DEditorElement(Live2DElement element, LayoutEditorScreen editor) { super(element, editor); }

    @Override public void init() {
        super.init();
        rightClickMenu.addClickableEntry("fancyl2d_live_preview", Component.translatable("fancyl2d.preview_enable"),
            (menu, entry) -> Live2DEditorPreview.toggle(editor, element))
            .setLabelSupplier((menu, entry) -> Component.translatable(
                Live2DEditorPreview.selected(editor, element) ? "fancyl2d.preview_disable" : "fancyl2d.preview_enable"));
        rightClickMenu.addClickableEntry("fancyl2d_choose_model", Component.translatable("fancyl2d.choose_model"),
            (menu, entry) -> openContextMenuScreen(new ModelPickerScreen(editor, path -> {
                if (!path.equals(element.model)) {
                    var before = createSnapshot();
                    element.model = path;
                    saveSnapshot(before);
                }
            })));
        field("model", e -> e.element.model, (e, v) -> e.element.model = v.trim(), "Folder/Name.model3.json");
        field("scale", e -> Double.toString(e.element.scale),
            (e, v) -> e.element.scale = Live2DSettings.number(v, e.element.scale, 0.1, 5), "1.0");
        field("follow", e -> Double.toString(e.element.follow),
            (e, v) -> e.element.follow = Live2DSettings.number(v, e.element.follow, 0, 1), "0.65");
        field("sway", e -> Double.toString(e.element.sway),
            (e, v) -> e.element.sway = Live2DSettings.number(v, e.element.sway, 0, 1), "0.0");
        field("fps", e -> Integer.toString(e.element.fps),
            (e, v) -> e.element.fps = Live2DSettings.frameRate(v, e.element.fps), "60");
        addToggleContextMenuEntryTo(rightClickMenu, "fancyl2d_idle", Live2DEditorElement.class,
            e -> e.element.idle, (e, enabled) -> e.element.idle = enabled, "fancyl2d.idle");
    }

    private void field(String key, ConsumingSupplier<Live2DEditorElement, String> getter,
                       BiConsumer<Live2DEditorElement, String> setter, String hint) {
        addStringInputContextMenuEntryTo(rightClickMenu, "fancyl2d_" + key, Live2DEditorElement.class,
            getter, setter, null, false, true, Component.translatable("fancyl2d." + key),
            true, hint, null, null);
    }

}
