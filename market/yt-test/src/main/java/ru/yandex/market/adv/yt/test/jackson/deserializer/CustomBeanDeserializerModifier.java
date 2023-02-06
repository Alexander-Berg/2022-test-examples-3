package ru.yandex.market.adv.yt.test.jackson.deserializer;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;

import ru.yandex.inside.yt.kosher.ytree.YTreeNode;

/**
 * Класс для выбора кастомных десериализаторов в процессе разбора json-файла.
 * Date: 13.01.2022
 * Project: arcadia-market_adv_adv-shop
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
public class CustomBeanDeserializerModifier extends BeanDeserializerModifier {

    @Override
    public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config,
                                                  BeanDescription beanDesc,
                                                  JsonDeserializer<?> deserializer) {
        if (beanDesc.getBeanClass() == YTreeNode.class) {
            return new YTreeNodeDeserializer();
        }

        return deserializer;
    }
}
