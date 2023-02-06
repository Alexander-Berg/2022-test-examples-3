package ru.yandex.market.mboc.app.proto;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.BaseMbocAppTest;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruIdMock;
import ru.yandex.market.mbo.solomon.SolomonPushService;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferUpdateSequenceService;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.http.MboCategoryOfferChanges.SimpleBaseOffer;
import ru.yandex.market.mboc.http.MboCategoryOfferChanges.SimpleServiceOffer;

public abstract class BaseOfferChangesServiceTest extends BaseMbocAppTest {
    protected static final String[] FIELDS_TO_COMPARE = {"businessId", "shopSku", "approvedMappingMskuId",
        "serviceOffersList", "isDeleted"};

    protected MboCategoryOfferChangesServiceImpl mboCategoryOfferChangesService;

    @Autowired
    protected OfferRepository offerRepository;
    @Autowired
    protected SupplierRepository supplierRepository;
    protected OfferUpdateSequenceService offerUpdateSequenceService;

    @Before
    public void setup() {
        offerUpdateSequenceService = new OfferUpdateSequenceService(
            jdbcTemplate,
            storageKeyValueService,
            Mockito.mock(SolomonPushService.class)
        );

        mboCategoryOfferChangesService = new MboCategoryOfferChangesServiceImpl(
            namedParameterJdbcTemplate,
            offerUpdateSequenceService,
            supplierRepository,
            new BeruIdMock()
        );

        // создаем данные
        supplierRepository.insertBatch(
            YamlTestUtil.readSuppliersFromResource("offer-changes/suppliers.yml"));
        offerRepository.insertOffers(
            YamlTestUtil.readOffersFromResources("offer-changes/offers.yml"));

        // проставляем modified_seq_id для данных инициализации
        offerUpdateSequenceService.copyOfferChangesFromStaging();
    }

    protected SimpleBaseOffer offer(int bizId, String ssku, int mskuId, int supplierId, int... supplierIds) {
        var builder = SimpleBaseOffer.newBuilder()
            .setBusinessId(bizId)
            .setShopSku(ssku)
            .setApprovedMappingMskuId(mskuId)
            .addServiceOffers(SimpleServiceOffer.newBuilder().setSupplierId(supplierId).build());
        Arrays.stream(supplierIds).forEach(id -> {
            builder.addServiceOffers(SimpleServiceOffer.newBuilder().setSupplierId(id).build());
        });
        return builder.build();
    }

    protected SimpleBaseOffer deletedOffer(int bizId, String ssku, int mskuId) {
        var builder = SimpleBaseOffer.newBuilder()
            .setBusinessId(bizId)
            .setShopSku(ssku)
            .setApprovedMappingMskuId(mskuId)
            .setIsDeleted(true);
        return builder.build();
    }

    protected static Comparator<Collection<SimpleServiceOffer>> getSupplierIdComparator() {
        return new Comparator<Collection<SimpleServiceOffer>>() {
            @Override
            public int compare(Collection<SimpleServiceOffer> o1, Collection<SimpleServiceOffer> o2) {
                var o1sorted = o1.stream().map(SimpleServiceOffer::getSupplierId).sorted()
                    .collect(Collectors.toList());
                var o2sorted = o2.stream().map(SimpleServiceOffer::getSupplierId).sorted()
                    .collect(Collectors.toList());
                return o1sorted.equals(o2sorted) ? 0 : 1;
            }
        };
    }
}
