package ru.yandex.market.adv.yt.test.jackson.deserializer;

import java.io.IOException;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.RequiredArgsConstructor;

import ru.yandex.inside.yt.kosher.impl.ytree.YTreeBooleanNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.adv.yt.test.jackson.deserializer.model.YTreeNodeJsonModel;

/**
 * Десериалайзер для преобразования информации из json-файла в {@link YTreeNode}.
 * Date: 13.01.2022
 * Project: arcadia-market_adv_adv-shop
 *
 * @author alexminakov
 */
@RequiredArgsConstructor
@ParametersAreNonnullByDefault
public class YTreeNodeDeserializer extends JsonDeserializer<YTreeNode> {

    @Override
    public YTreeNode deserialize(JsonParser p,
                                 DeserializationContext context) throws IOException {
        YTreeNodeJsonModel model = p.readValueAs(YTreeNodeJsonModel.class);

        if (model == null) {
            return new YTreeStringNodeImpl("", Map.of());
        } else {
            if (model.isBool()) {
                return new YTreeBooleanNodeImpl(Boolean.parseBoolean(model.getValue()), Map.of());
            } else if (model.isInteger()) {
                return new YTreeIntegerNodeImpl(model.isSigned(), Long.parseLong(model.getValue()), Map.of());
            } else {
                return new YTreeStringNodeImpl(model.getValue(), Map.of());
            }
        }
    }
}
