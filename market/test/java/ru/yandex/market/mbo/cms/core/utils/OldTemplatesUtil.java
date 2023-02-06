package ru.yandex.market.mbo.cms.core.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.yandex.common.util.StringUtils;
import ru.yandex.market.mbo.cms.core.dao.model.PropertyLine;
import ru.yandex.market.mbo.cms.core.models.Constants;
import ru.yandex.market.mbo.cms.core.models.NodeType;
import ru.yandex.market.mbo.cms.core.models.PlaceholderUtils;

//TODO непонятно, почему этот класс deprecated. На замену ничего нету.
// Тут либо должна быть замена либо он не должен быть deprecated.
@Deprecated
public class OldTemplatesUtil {

    private OldTemplatesUtil() {
    }

    public static NodeType makeBadNodeType(String name, String templateAsStr, String properties) {
        return makeBadNodeType(name, Constants.Device.DESKTOP, Constants.Format.JSON, templateAsStr, properties);
    }

    public static NodeType makeBadNodeType(String name, Constants.Device device, Constants.Format format,
                                           String templateAsStr, String properties) {
        NodeType nodeType = new NodeType(name);
        if (templateAsStr != null) {

            Map<Constants.Device, Map<Constants.Format, String>> templates = nodeType.getTemplates();
            if (templates == null) {
                templates = new HashMap<>();
            }
            templates.computeIfAbsent(device, k -> new HashMap<>()).put(format, templateAsStr);

            nodeType.setTemplates(templates);

            PlaceholderUtils.extractPlaceholders(templateAsStr)
                .forEach((key, value) -> nodeType.addField(value.getName(), value)); //todo e.getKey()???
        }
        addProperties(nodeType, properties);
        return nodeType;
    }

    public static void addProperties(NodeType nodeType, String properties) {
        if (!StringUtils.isEmpty(properties)) {
            convertPlaceholderProperties(properties).forEach(pl -> {
                if (nodeType.getNamePointer().equals(pl.getPath())) {
                    nodeType.addProperty(pl.getPropertyName(), pl.getValues());
                } else if (nodeType.getFieldsNames().contains(pl.getPath())) {
                    nodeType.getField(pl.getPath()).getProperties().put(pl.getPropertyName(), pl.getValues());
                } else {
                    nodeType.addPropertyBranch(pl.getPath(), pl.getPropertyName(), pl.getValues());
                }
            });
        }
    }

    public static List<PropertyLine> convertPlaceholderProperties(String properties) {
        return Stream.of(properties.split("\\n"))
                .filter(s -> !StringUtils.isEmpty(s))
                .map(PropertyLine::fromString)
                .collect(Collectors.toList());
    }
}
