package ru.yandex.crypta.graph2.matching.human.workflow.component.ops.indevice;

import ru.yandex.crypta.graph.soup.config.Soup;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph.soup.config.proto.TEdgeProps;
import ru.yandex.crypta.graph.soup.config.proto.TEdgeRecord;
import ru.yandex.crypta.graph2.model.soup.edge.EdgeType;
import ru.yandex.crypta.graph2.model.soup.sources.EdgeTypeConfigProvider;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

public class TestIndeviceEdgeTypeConfigProvider implements EdgeTypeConfigProvider {

    private static final TEdgeRecord INDEV_PROPS = TEdgeRecord.newBuilder()
            .setProps(TEdgeProps.newBuilder()
                    .setDeviceBounds(TEdgeProps.EDeviceBounds.INDEVICE)
            ).build();

    private static final TEdgeRecord USUAL_PROPS = TEdgeRecord.newBuilder().build();

    @Override
    public TEdgeRecord getEdgeTypeConfig(EdgeType edgeType) {
        // considers only indevice edge types
        if (edgeType.equals(new EdgeType(
                EIdType.PUID, EIdType.IDFA,
                ESourceType.APP_PASSPORT_AUTH, ELogSourceType.OAUTH_LOG))) {
            return INDEV_PROPS;
        }
        if (edgeType.equals(new EdgeType(
                EIdType.PUID, EIdType.UUID,
                ESourceType.APP_PASSPORT_AUTH, ELogSourceType.OAUTH_LOG))) {
            return INDEV_PROPS;
        }
        if (edgeType.equals(new EdgeType(
                EIdType.YANDEXUID, EIdType.UUID,
                ESourceType.YABRO_IOS, ELogSourceType.SBAPI_ACCESS_LOG))) {
            return INDEV_PROPS;
        }
        if (edgeType.equals(new EdgeType(
                EIdType.YANDEXUID, EIdType.MM_DEVICE_ID_HASH,
                ESourceType.APP_METRICA_INSTALL_TRACKING, ELogSourceType.MOBILE_TRACKING_LOG))) {
            return INDEV_PROPS;
        }
        if (edgeType.equals(new EdgeType(
                EIdType.IDFA, EIdType.MAC,
                ESourceType.APP_METRICA, ELogSourceType.METRIKA_MOBILE_LOG))) {
            return INDEV_PROPS;
        }
        if (edgeType.equals(new EdgeType(
                EIdType.UUID, EIdType.IDFA,
                ESourceType.APP_METRICA, ELogSourceType.METRIKA_MOBILE_LOG))) {
            return INDEV_PROPS;
        }
        return USUAL_PROPS;
    }

    @Override
    public TEdgeRecord getEdgeTypeConfig(ru.yandex.crypta.graph2.model.soup.proto.EdgeType edgeType) {
        EdgeType edgeType1 = new EdgeType(
                Soup.CONFIG.getIdType(edgeType.getId1Type()).getType(),
                Soup.CONFIG.getIdType(edgeType.getId2Type()).getType(),
                Soup.CONFIG.getSourceType(edgeType.getSourceType()).getType(),
                Soup.CONFIG.getLogSource(edgeType.getLogSource()).getType()
        );
        return getEdgeTypeConfig(edgeType1);
    }
}
