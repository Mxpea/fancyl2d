package com.aurelith.fancyl2d.client;

import java.util.Objects;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import net.minecraft.network.chat.Component;

final class Live2DElementBuilder extends ElementBuilder<Live2DElement, Live2DEditorElement> {
    Live2DElementBuilder() { super("fancyl2d_model"); }

    @Override public Live2DElement buildDefaultInstance() { return new Live2DElement(this); }

    @Override public Live2DElement deserializeElement(SerializedElement input) {
        var element = buildDefaultInstance();
        element.model = Objects.requireNonNullElse(input.getValue("live2d_model"), Live2DSettings.DEFAULT.model());
        element.scale = Live2DSettings.number(input.getValue("live2d_scale"), 1, 0.1, 5);
        element.follow = Live2DSettings.number(input.getValue("live2d_follow"), 0.65, 0, 1);
        element.idle = deserializeBoolean(true, input.getValue("live2d_idle"));
        element.sway = Live2DSettings.number(input.getValue("live2d_sway"), Live2DSettings.DEFAULT.sway(), 0, 1);
        element.fps = Live2DSettings.frameRate(input.getValue("live2d_fps"), Live2DSettings.DEFAULT.fps());
        return element;
    }

    @Override protected SerializedElement serializeElement(Live2DElement element, SerializedElement output) {
        output.putProperty("live2d_model", element.model);
        output.putProperty("live2d_scale", element.scale);
        output.putProperty("live2d_follow", element.follow);
        output.putProperty("live2d_idle", element.idle);
        output.putProperty("live2d_sway", element.sway);
        output.putProperty("live2d_fps", element.fps);
        return output;
    }

    @Override public Live2DEditorElement wrapIntoEditorElement(Live2DElement element, LayoutEditorScreen editor) {
        return new Live2DEditorElement(element, editor);
    }

    @Override public Component getDisplayName(AbstractElement element) {
        return Component.translatable("fancyl2d.element");
    }

    @Override public Component[] getDescription(AbstractElement element) {
        return new Component[]{Component.translatable("fancyl2d.description")};
    }
}
