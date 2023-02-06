package ru.yandex.market.mbo.mdm.tms.executors;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.business.BusinessDto;
import ru.yandex.market.mbi.api.client.entity.business.BusinessListDto;
import ru.yandex.market.mbi.api.client.entity.fulfillment.SupplierInfo;
import ru.yandex.market.mbi.api.client.entity.partner.FmcgPartnerInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;

/**
 * @author dmserebr
 * @date 22/07/2020
 */
@SuppressWarnings("checkstyle:magicNumber")
public class MdmSupplierImportExecutorTest extends MdmBaseDbTestClass {
    private static final int SUPPLIER_ID_1 = 10263700;
    private static final int SUPPLIER_ID_2 = 10264169;
    private static final int SUPPLIER_ID_3 = 9191919;
    private static final int SUPPLIER_ID_4 = 12345;
    private static final int SUPPLIER_ID_5 = 123;
    private static final int FMCG1 = 10301329;
    private static final int FMCG2 = 10301529;
    private static final int FMCG3 = 10301629;
    private static final int FMCG4 = 10301330;
    private static final int BUSINESS1 = 593094;
    private static final int BUSINESS2 = 593095;
    private static final int BUSINESS3 = 593096;
    private static final int BUSINESS4 = 593097;

    private MdmSupplierImportExecutor mdmSupplierImportExecutor;

    private MbiApiClient mbiApiClient;

    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;

    @Before
    public void before() {
        mbiApiClient = Mockito.mock(MbiApiClient.class);
        mdmSupplierImportExecutor = new MdmSupplierImportExecutor(mbiApiClient, mdmSupplierRepository);
    }

    @Test
    public void testLoadSuppliersFromMbi() {
        MdmSupplier supplier1 = createSupplier(SUPPLIER_ID_1, null, MdmSupplierType.THIRD_PARTY, null);
        MdmSupplier supplier2 = createSupplier(SUPPLIER_ID_2, "Беру", MdmSupplierType.FIRST_PARTY, null);
        MdmSupplier supplier3 = createSupplier(SUPPLIER_ID_3, null, MdmSupplierType.REAL_SUPPLIER, "09010901");
        MdmSupplier supplier4 = createSupplier(SUPPLIER_ID_4, null, MdmSupplierType.THIRD_PARTY, null);

        mdmSupplierRepository.insertBatch(List.of(supplier1, supplier2, supplier3, supplier4));

        Instant ts1 = mdmSupplierRepository.findById(SUPPLIER_ID_1).getUpdatedTs();
        Instant ts2 = mdmSupplierRepository.findById(SUPPLIER_ID_2).getUpdatedTs();

        // API returns names and one new supplier, but one old is missing
        Mockito.when(mbiApiClient.getSupplierInfoList(Mockito.any())).thenReturn(generateRichMbiSuppliers());
        Mockito.when(mbiApiClient.getAllFmcgPartners()).thenReturn(generateFmcgSupplier());
        Mockito.when(mbiApiClient.getAllBusinesses()).thenReturn(generateBusinessSupplier());

        mdmSupplierImportExecutor.execute();

        Map<Integer, MdmSupplier> updated = mdmSupplierRepository.findAll().stream()
            .collect(Collectors.toMap(MdmSupplier::getId, Function.identity()));

        Assertions.assertThat(updated).hasSize(7);
        Assertions.assertThat(updated.values())
            .usingElementComparatorIgnoringFields("updatedTs", "businessStateUpdatedTs")
            .containsExactlyInAnyOrder(
                createSupplier(SUPPLIER_ID_1, " EReznikova синий магазин", MdmSupplierType.THIRD_PARTY, null),
                createSupplier(SUPPLIER_ID_2, "Беру", MdmSupplierType.FIRST_PARTY, null),
                createSupplier(SUPPLIER_ID_3, "all fields", MdmSupplierType.REAL_SUPPLIER, "09010901"),
                createSupplier(SUPPLIER_ID_4, null, MdmSupplierType.THIRD_PARTY, null, true),
                createSupplier(SUPPLIER_ID_5, "test", MdmSupplierType.THIRD_PARTY, null),
                createSupplier(FMCG1, "Супер-Вкусвилл", MdmSupplierType.FMCG, null),
                createSupplier(BUSINESS1, "ПАО 'СТАВКИ НА СПОООРТ'", MdmSupplierType.BUSINESS, null)
            );

        Assertions.assertThat(updated.get(SUPPLIER_ID_1).getUpdatedTs()).isAfter(ts1);
        Assertions.assertThat(updated.get(SUPPLIER_ID_2).getUpdatedTs()).isEqualTo(ts2);

        // Supplier 4 resurrects
        Mockito.when(mbiApiClient.getSupplierInfoList(Mockito.any())).thenReturn(
            Stream.concat(
                generateRichMbiSuppliers().stream(),
                Stream.of(new SupplierInfo.Builder()
                    .setId(SUPPLIER_ID_4)
                    .setName("test 2")
                    .setSupplierType(SupplierType.THIRD_PARTY)
                    .build()))
                .collect(Collectors.toList()));

        mdmSupplierImportExecutor.execute();

        Map<Integer, MdmSupplier> updatedAgain = mdmSupplierRepository.findAll().stream()
            .collect(Collectors.toMap(MdmSupplier::getId, Function.identity()));

        Assertions.assertThat(updatedAgain).hasSize(7);
        // the value is no longer marked as deleted
        Assertions.assertThat(updatedAgain.get(SUPPLIER_ID_4)).isEqualToIgnoringGivenFields(
            createSupplier(SUPPLIER_ID_4, "test 2", MdmSupplierType.THIRD_PARTY, null),
            "updatedTs", "businessStateUpdatedTs");
    }

    @Test
    public void whenExistingSupplierHasBusinessFlagThenImportShouldNotResetIt() {
        MdmSupplier supplier1 = createSupplier(SUPPLIER_ID_1, null, MdmSupplierType.THIRD_PARTY, null);
        MdmSupplier supplier2 = createSupplier(SUPPLIER_ID_2, "Беру", MdmSupplierType.FIRST_PARTY, null);
        supplier1.setBusinessEnabled(false);
        supplier2.setBusinessEnabled(true);
        mdmSupplierRepository.insertBatch(List.of(supplier1, supplier2));

        Mockito.when(mbiApiClient.getAllFmcgPartners()).thenReturn(generateFmcgSupplier());
        Mockito.when(mbiApiClient.getAllBusinesses()).thenReturn(generateBusinessSupplier());
        Mockito.when(mbiApiClient.getSupplierInfoList(Mockito.any())).thenReturn(List.of(
            new SupplierInfo.Builder()
                .setId(SUPPLIER_ID_1)
                .setName("ЭкстримЛеммингСпортсЕкуипментИнкорпорейтедКомпаниБизнесс")
                .setSupplierType(SupplierType.THIRD_PARTY)
                .build(),
            new SupplierInfo.Builder()
                .setId(SUPPLIER_ID_2)
                .setName("ИП Ипов И.П.")
                .setSupplierType(SupplierType.FIRST_PARTY)
                .build()));
        mdmSupplierImportExecutor.execute();

        Map<Integer, MdmSupplier> updated = mdmSupplierRepository.findAll().stream()
            .collect(Collectors.toMap(MdmSupplier::getId, Function.identity()));

        Assertions.assertThat(updated).hasSize(4);
        Assertions.assertThat(updated.values())
            .usingElementComparatorIgnoringFields("updatedTs", "businessStateUpdatedTs")
            .containsExactlyInAnyOrder(
                createSupplier(SUPPLIER_ID_1, "ЭкстримЛеммингСпортсЕкуипментИнкорпорейтедКомпаниБизнесс",
                    MdmSupplierType.THIRD_PARTY, null).setBusinessEnabled(false),
                createSupplier(SUPPLIER_ID_2, "ИП Ипов И.П.",
                    MdmSupplierType.FIRST_PARTY, null).setBusinessEnabled(true),
                createSupplier(FMCG1, "Супер-Вкусвилл", MdmSupplierType.FMCG, null),
                createSupplier(BUSINESS1, "ПАО 'СТАВКИ НА СПОООРТ'", MdmSupplierType.BUSINESS, null)
            );
    }

    @Test
    public void testRepeatedImportDoesNotSetDeletedFlag() {
        Mockito.when(mbiApiClient.getSupplierInfoList(Mockito.any())).thenReturn(generateRichMbiSuppliers());
        Mockito.when(mbiApiClient.getAllFmcgPartners()).thenReturn(generateFmcgSupplier());
        Mockito.when(mbiApiClient.getAllBusinesses()).thenReturn(generateBusinessSuppliers());

        mdmSupplierImportExecutor.execute();
        mdmSupplierImportExecutor.execute();

        List<Boolean> deletedFlags = mdmSupplierRepository.findAll().stream()
            .map(MdmSupplier::isDeleted)
            .collect(Collectors.toList());

        Assertions.assertThat(deletedFlags).containsOnly(false);
    }

    @Test
    public void testUndoDeleteBusinesses() {
        mdmSupplierRepository.insertBatch(
            new MdmSupplier().setId(BUSINESS1).setType(MdmSupplierType.BUSINESS).setDeleted(true).setName("biz1"),
            new MdmSupplier().setId(BUSINESS2).setType(MdmSupplierType.BUSINESS).setDeleted(false).setName("biz2"),
            new MdmSupplier().setId(BUSINESS3).setType(MdmSupplierType.BUSINESS).setDeleted(false).setName("biz3")
        );

        Mockito.when(mbiApiClient.getSupplierInfoList(Mockito.any())).thenReturn(generateRichMbiSuppliers());
        Mockito.when(mbiApiClient.getAllFmcgPartners()).thenReturn(generateFmcgSupplier());
        Mockito.when(mbiApiClient.getAllBusinesses()).thenReturn(generateBusinessSuppliers());

        mdmSupplierImportExecutor.execute();
        List<MdmSupplier> found = mdmSupplierRepository.findAllOfTypes(MdmSupplierType.BUSINESS);
        Assertions.assertThat(found).usingElementComparatorIgnoringFields("updatedTs", "businessStateUpdatedTs")
            .containsExactlyInAnyOrder(
                new MdmSupplier().setId(BUSINESS1).setType(MdmSupplierType.BUSINESS).setDeleted(false).setName("biz1"),
                new MdmSupplier().setId(BUSINESS2).setType(MdmSupplierType.BUSINESS).setDeleted(false).setName("biz2"),
                new MdmSupplier().setId(BUSINESS3).setType(MdmSupplierType.BUSINESS).setDeleted(true).setName("biz3"),
                new MdmSupplier().setId(BUSINESS4).setType(MdmSupplierType.BUSINESS).setDeleted(false).setName("biz4")
            );
    }

    @Test
    public void testUndoDeleteFmcg() {
        mdmSupplierRepository.insertBatch(
            new MdmSupplier().setId(FMCG1).setType(MdmSupplierType.FMCG).setDeleted(true).setName("1"),
            new MdmSupplier().setId(FMCG2).setType(MdmSupplierType.FMCG).setDeleted(false).setName("2"),
            new MdmSupplier().setId(FMCG3).setType(MdmSupplierType.FMCG).setDeleted(false).setName("3")
        );

        Mockito.when(mbiApiClient.getSupplierInfoList(Mockito.any())).thenReturn(generateRichMbiSuppliers());
        Mockito.when(mbiApiClient.getAllFmcgPartners()).thenReturn(generateFmcgSuppliers());
        Mockito.when(mbiApiClient.getAllBusinesses()).thenReturn(generateBusinessSupplier());

        mdmSupplierImportExecutor.execute();
        List<MdmSupplier> found = mdmSupplierRepository.findAllOfTypes(MdmSupplierType.FMCG);
        Assertions.assertThat(found).usingElementComparatorIgnoringFields("updatedTs", "businessStateUpdatedTs")
            .containsExactlyInAnyOrder(
                new MdmSupplier().setId(FMCG1).setType(MdmSupplierType.FMCG).setDeleted(false).setName("1"),
                new MdmSupplier().setId(FMCG2).setType(MdmSupplierType.FMCG).setDeleted(false).setName("2"),
                new MdmSupplier().setId(FMCG3).setType(MdmSupplierType.FMCG).setDeleted(true).setName("3"),
                new MdmSupplier().setId(FMCG4).setType(MdmSupplierType.FMCG).setDeleted(false).setName("4")
            );
    }

    @Test
    public void testUndoDeleteRegularSuppliers() {
        mdmSupplierRepository.insertBatch(
            new MdmSupplier().setId(SUPPLIER_ID_1).setType(MdmSupplierType.THIRD_PARTY).setDeleted(true).setName("1"),
            new MdmSupplier().setId(SUPPLIER_ID_2).setType(MdmSupplierType.THIRD_PARTY).setDeleted(false).setName("2"),
            new MdmSupplier().setId(SUPPLIER_ID_3).setType(MdmSupplierType.THIRD_PARTY).setDeleted(false).setName("3")
        );

        Mockito.when(mbiApiClient.getSupplierInfoList(Mockito.any())).thenReturn(generateMbiSuppliers());
        Mockito.when(mbiApiClient.getAllFmcgPartners()).thenReturn(generateFmcgSupplier());
        Mockito.when(mbiApiClient.getAllBusinesses()).thenReturn(generateBusinessSupplier());

        mdmSupplierImportExecutor.execute();
        List<MdmSupplier> found = mdmSupplierRepository.findAllOfTypes(MdmSupplierType.THIRD_PARTY);
        Assertions.assertThat(found).usingElementComparatorIgnoringFields("updatedTs", "businessStateUpdatedTs")
            .containsExactlyInAnyOrder(
                new MdmSupplier().setId(SUPPLIER_ID_1).setType(MdmSupplierType.THIRD_PARTY).setDeleted(false).setName("1"),
                new MdmSupplier().setId(SUPPLIER_ID_2).setType(MdmSupplierType.THIRD_PARTY).setDeleted(false).setName("2"),
                new MdmSupplier().setId(SUPPLIER_ID_3).setType(MdmSupplierType.THIRD_PARTY).setDeleted(true).setName(
                    "3"),
                new MdmSupplier().setId(SUPPLIER_ID_4).setType(MdmSupplierType.THIRD_PARTY).setDeleted(false).setName("4")
            );
    }

    @Test
    public void skipInvalidSuppliersFromMbi() {
        // API returns one business supplier with big ID
        Mockito.when(mbiApiClient.getSupplierInfoList(Mockito.any())).thenReturn(generateMbiSuppliers());
        Mockito.when(mbiApiClient.getAllFmcgPartners()).thenReturn(generateFmcgSupplier());
        Mockito.when(mbiApiClient.getAllBusinesses()).thenReturn(generateBusinessSuppliersWithInvalid());

        mdmSupplierImportExecutor.execute();

        Map<Integer, MdmSupplier> updated = mdmSupplierRepository.findAll()
            .stream()
            .collect(Collectors.toMap(MdmSupplier::getId, Function.identity()));

        Assertions.assertThat(updated).hasSize(5);
        Assertions.assertThat(updated.values())
            .usingElementComparatorIgnoringFields("updatedTs", "businessStateUpdatedTs")
            .containsExactlyInAnyOrder(
                createSupplier(SUPPLIER_ID_1, "1", MdmSupplierType.THIRD_PARTY, null),
                createSupplier(SUPPLIER_ID_2, "2", MdmSupplierType.THIRD_PARTY, null),
                createSupplier(SUPPLIER_ID_4, "4", MdmSupplierType.THIRD_PARTY, null),
                createSupplier(FMCG1, "Супер-Вкусвилл", MdmSupplierType.FMCG, null),
                createSupplier(BUSINESS1, "biz1", MdmSupplierType.BUSINESS, null)
            );
    }

    private BusinessListDto generateBusinessSupplier() {
        return new BusinessListDto(List.of(
            new BusinessDto(BUSINESS1, "ПАО 'СТАВКИ НА СПОООРТ'")
        ));
    }

    private BusinessListDto generateBusinessSuppliers() {
        return new BusinessListDto(List.of(
            new BusinessDto(BUSINESS1, "biz1"),
            new BusinessDto(BUSINESS2, "biz2"),
            new BusinessDto(BUSINESS4, "biz4")
        ));
    }

    private BusinessListDto generateBusinessSuppliersWithInvalid() {
        return new BusinessListDto(List.of(
            new BusinessDto(BUSINESS1, "biz1"),
            new BusinessDto(Long.MAX_VALUE, "bizInvalid")
        ));
    }

    private List<FmcgPartnerInfo> generateFmcgSupplier() {
        return List.of(
            new FmcgPartnerInfo(FMCG1, "Супер-Вкусвилл", "ИНТЕРНЕТ РЕШЕНИЯ", OrganizationType.OOO, null)
        );
    }

    private List<FmcgPartnerInfo> generateFmcgSuppliers() {
        return List.of(
            new FmcgPartnerInfo(FMCG1, "1", null, OrganizationType.OOO, null),
            new FmcgPartnerInfo(FMCG2, "2", null, OrganizationType.OOO, null),
            new FmcgPartnerInfo(FMCG4, "4", null, OrganizationType.OOO, null)
        );
    }

    private List<SupplierInfo> generateMbiSuppliers() {
        return List.of(
            new SupplierInfo.Builder()
                .setId(SUPPLIER_ID_1)
                .setName("1")
                .setSupplierType(SupplierType.THIRD_PARTY)
                .build(),
            new SupplierInfo.Builder()
                .setId(SUPPLIER_ID_2)
                .setName("2")
                .setSupplierType(SupplierType.THIRD_PARTY)
                .build(),
            new SupplierInfo.Builder()
                .setId(SUPPLIER_ID_4)
                .setName("4")
                .setSupplierType(SupplierType.THIRD_PARTY)
                .build()
        );
    }

    private List<SupplierInfo> generateRichMbiSuppliers() {
        return List.of(
            new SupplierInfo.Builder()
                .setId(SUPPLIER_ID_1)
                .setName(" EReznikova синий магазин")
                .setSupplierType(SupplierType.THIRD_PARTY)
                .build(),
            new SupplierInfo.Builder()
                .setId(SUPPLIER_ID_2)
                .setName("Беру")
                .setSupplierType(SupplierType.FIRST_PARTY)
                .build(),
            new SupplierInfo.Builder()
                .setId(SUPPLIER_ID_3)
                .setRsId("09010901")
                .setName("all fields")
                .setSupplierType(SupplierType.REAL_SUPPLIER)
                .build(),
            new SupplierInfo.Builder()
                .setId(SUPPLIER_ID_5)
                .setName("test")
                .setSupplierType(SupplierType.THIRD_PARTY)
                .build()
        );
    }

    private static MdmSupplier createSupplier(int id, String name, MdmSupplierType type, String rsId) {
        return createSupplier(id, name, type, rsId, false);
    }

    private static MdmSupplier createSupplier(int id, String name, MdmSupplierType type, String rsId, boolean deleted) {
        var supplier = new MdmSupplier();
        supplier.setId(id);
        supplier.setName(name);
        supplier.setType(type);
        supplier.setRealSupplierId(rsId);
        supplier.setDeleted(deleted);
        return supplier;
    }
}
