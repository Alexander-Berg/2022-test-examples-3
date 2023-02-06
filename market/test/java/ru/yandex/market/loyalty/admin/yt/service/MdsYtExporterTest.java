package ru.yandex.market.loyalty.admin.yt.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.loyalty.admin.mds.BytesContentConsumer;
import ru.yandex.market.loyalty.admin.mds.DirectoryEntry;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.tms.MdsSnapshotProcessor;
import ru.yandex.market.loyalty.admin.yt.dao.WhiteBlackListYtDao;
import ru.yandex.market.loyalty.core.config.YtArnold;
import ru.yandex.market.loyalty.core.config.YtHahn;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.isA;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.loyalty.admin.utils.YtTestHelper.ytPathToName;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.UPLOAD_WHITE_AND_BLACK_LIST_TO_YT;

public class MdsYtExporterTest extends MarketLoyaltyAdminMockedDbTest {

    @Autowired
    @YtHahn
    private Yt ytHahn;
    @Autowired
    @YtArnold
    private Yt ytArnold;
    @Captor
    private ArgumentCaptor<Iterator<JsonNode>> argumentCaptor;
    @Captor
    private ArgumentCaptor<YTreeMapNodeImpl> argumentCaptorAttributeMap;
    @Autowired
    private MdsSnapshotProcessor mdsSnapshotProcessor;
    @Autowired
    private MdsS3Client mdsS3Client;
    @Value("${market.loyalty.mds.prefix}")
    private String mdsPrefix;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private ConfigurationService configurationService;

    @Before
    public void mockTransactionHandling() {
        when(ytHahn.transactions().start(any(Optional.class), anyBoolean(), any(Duration.class), any(Map.class)))
                .thenReturn(GUID.valueOf("1-8e9c4f69-a3bc6964-3e99ab10"));
        when(ytArnold.transactions().start(any(Optional.class), anyBoolean(), any(Duration.class), any(Map.class)))
                .thenReturn(GUID.valueOf("1-8e9c4f69-a3bc6964-3e99ab10"));
        configurationService.set(UPLOAD_WHITE_AND_BLACK_LIST_TO_YT, true);
    }

    @Test
    public void shouldExportActivePromo() throws Exception {
        promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixedPromocode()
                        .addCoinRule(RuleType.SUPPLIER_FILTER_RULE, RuleParameterName.SUPPLIER_ID, Set.of(1234L))
                        .setActionCode("SAMSUNG500")
        );

        mockMds();

        verifyHasPromo();
    }

    private void verifyHasPromo() {
        verify(ytHahn.tables(), times(1)).write(
                any(Optional.class),
                anyBoolean(),
                argThat(ytPathToName(endsWith(WhiteBlackListYtDao.TMP_TABLE_NAME))),
                eq(YTableEntryTypes.JACKSON_UTF8),
                argumentCaptor.capture()
        );
        verify(ytHahn.cypress(), times(1)).set(
                any(Optional.class),
                anyBoolean(),
                any(YPath.class),
                argumentCaptorAttributeMap.capture()
        );
        checkCapturedPromo(argumentCaptor.getAllValues());
        checkAttribute(argumentCaptorAttributeMap.getAllValues());
    }

    private void checkCapturedPromo(List<Iterator<JsonNode>> captured) {
        ImmutableList.Builder<JsonNode> nodesBuilder = ImmutableList.builder();
        for (Iterator<JsonNode> it : captured) {
            it.forEachRemaining(nodesBuilder::add);
        }
        List<JsonNode> nodes = nodesBuilder.build();
        Assertions.assertEquals(nodes.size(), 1);
        Assertions.assertFalse(nodes.get(0).get("promo").isEmpty());
    }

    private void checkAttribute(List<YTreeMapNodeImpl> allValues) {
        YTreeNode protobuf = allValues.get(0).getAttributes()
                .get("_yql_proto_field_promo");
        Assertions.assertTrue(protobuf.stringValue().startsWith("{\"name\":\"Market.Loyalty.FastPipeLine.MetaInfo\""));
    }

    private void mockMds() throws Exception {
        when(mdsS3Client.download(
                argThat(hasProperty("key", equalTo(mdsPrefix + DirectoryEntry.HASHES.getFileName()))),
                argThat(isA(BytesContentConsumer.class))
        ))
                .thenReturn("".getBytes(StandardCharsets.ISO_8859_1));

        mdsSnapshotProcessor.process();

        ArgumentCaptor<ResourceLocation> resourceLocationCaptor = ArgumentCaptor.forClass(ResourceLocation.class);
        ArgumentCaptor<ContentProvider> contentProviderCaptor = ArgumentCaptor.forClass(ContentProvider.class);
        int directoryEntriesCnt = DirectoryEntry.values().length;
        verify(mdsS3Client, times(directoryEntriesCnt)).upload(
                resourceLocationCaptor.capture(),
                contentProviderCaptor.capture()
        );
    }
}
