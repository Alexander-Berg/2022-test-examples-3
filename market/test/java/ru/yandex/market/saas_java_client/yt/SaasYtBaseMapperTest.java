package ru.yandex.market.saas_java_client.yt;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Test;
import ru.yandex.inside.yt.kosher.impl.operations.StatisticsImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.operations.Yield;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.saas_java_client.http.common.SaasAttr;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.DoubleKind;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.Group;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.IntKind;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.IsProperty;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.NoGroup;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.Search;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.StringKind;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.Zone;
import ru.yandex.search.saas.RTYServer;
import ru.yandex.search.saas.RTYServer.TMessage.TDocument;
import ru.yandex.search.saas.RTYServer.TMessage.TMessageType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("checkstyle:MagicNumber")
public class SaasYtBaseMapperTest {
    private SaasAttr<IntKind, Search, Group, IsProperty> intAttr =
            SaasAttr.intAttr("i_test").property().group().search();
    private SaasAttr<StringKind, Search, Group, IsProperty> strAttr =
            SaasAttr.stringAttr("s_test").property().group().search();
    private SaasAttr<StringKind, Zone, NoGroup, IsProperty> zoneAttr =
            SaasAttr.stringAttr("z_test").property().zone();
    private SaasAttr<DoubleKind, Search, NoGroup, IsProperty> doubleAttr =
            SaasAttr.doubleAttr("d_test").property().search();

    @Test
    public void testMapper() throws InvalidProtocolBufferException {
        YTreeNode input = YTree.mapBuilder()
                .key("test").value("any")
                .endMap()
                .build();

        AtomicReference<YTreeMapNode> holder = new AtomicReference<>();
        Mapper mapper = new Mapper();
        mapper.start(null, null);
        mapper.map((YTreeMapNode) input, new Yield<YTreeMapNode>() {
            @Override
            public void yield(int index, YTreeMapNode value) {
                holder.set(value);
            }

            @Override
            public void close() {
            }
        }, new StatisticsImpl());


        YTreeMapNode result = holder.get();
        assertEquals("00006649", result.getString("key"));
        assertEquals("The key", result.getString("subkey"));

        RTYServer.TMessage message = RTYServer.TMessage.parseFrom(result.getBytes("value"));
        TDocument document = message.getDocument();
        assertEquals(TMessageType.ADD_DOCUMENT, message.getMessageType());
        assertEquals(42L, document.getKeyPrefix());
        assertEquals(4, document.getDocumentPropertiesList().size());
        assertEquals(1, document.getRootZone().getChildrenCount());
        assertEquals("Zone! value", document.getRootZone().getChildren(0).getText());

        List<TDocument.TProperty> properties = document.getDocumentPropertiesList();
        Map<String, String> props = properties.stream()
                .collect(Collectors.toMap(TDocument.TProperty::getName, TDocument.TProperty::getValue));
        assertEquals("String value", props.get(strAttr.getName()));
        assertEquals("42", props.get(intAttr.getName()));
        assertEquals("12.34", props.get(doubleAttr.getName()));
    }

    class Mapper extends SaasYtBaseMapper {
        @Override
        protected Long getKeyPrefix(YTreeMapNode input) {
            return 42L;
        }

        @Override
        protected String getKey(YTreeMapNode input) {
            return "The key";
        }

        @Override
        protected void fillDocument(YTreeMapNode input) {
            addIntAttr(intAttr, 42);
            addStrAttr(strAttr, "String value");
            addStrAttr(zoneAttr, "Zone! value");
            addDoubleAttr(doubleAttr, 12.34);
        }
    }
}
