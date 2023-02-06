package ru.yandex.market.mbo.db.vendor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.configs.db.category.VendorGoodContentExclusionConfig;
import ru.yandex.market.mbo.core.category.VendorGoodContentExclusionService;
import ru.yandex.market.mbo.core.kdepot.api.KnowledgeDepotService;
import ru.yandex.market.mbo.db.SizeMeasureService;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkServiceInterface;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.db.params.ParameterLoaderService;
import ru.yandex.market.mbo.gwt.models.linkedvalues.LinkDirection;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLink;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLinkSearchCriteria;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLinkType;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.vendor.Filter;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;
import ru.yandex.market.mbo.gwt.models.visual.Word;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class GlobalVendorServiceTest {
    private static final long TEST_UID = 322;
    private static final Word NAME = new Word(Word.DEFAULT_LANG_ID, "vendorName", false);
    private static final Word ALIAS1 = new Word(Word.DEFAULT_LANG_ID, "alias1", false);
    private static final Word ALIAS2 = new Word(Word.DEFAULT_LANG_ID, "alias2", false);
    private static final Word EMPTY_ALIAS = new Word(Word.DEFAULT_LANG_ID, "", false);
    private static final Word NULL_ALIAS = null;

    private Filter nameFilter;
    private GlobalVendorDBMock vendorDb;
    private GlobalVendorLoaderService globalVendorLoaderService;
    private GlobalVendorService service;
    private IParameterLoaderService parameterLoaderService;

    private ValueLinkServiceInterface linkServiceInterfaceMock;


    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        nameFilter = new Filter();
        vendorDb = new GlobalVendorDBMock();
        globalVendorLoaderService = new GlobalVendorLoaderService();
        service = new GlobalVendorService();


        GlobalVendorUtilDBMock vendorDBUtil = new GlobalVendorUtilDBMock(vendorDb);
        linkServiceInterfaceMock = Mockito.mock(ValueLinkServiceInterface.class);
        parameterLoaderService = Mockito.mock(ParameterLoaderService.class);

        globalVendorLoaderService.setValueLinkService(linkServiceInterfaceMock);
        globalVendorLoaderService.setVendorDb(vendorDb);
        globalVendorLoaderService.setVendorDBUtil(vendorDBUtil);
        globalVendorLoaderService.setParameterLoaderService(parameterLoaderService);
        service.setValueLinkService(linkServiceInterfaceMock);
        service.setVendorDb(vendorDb);
        service.setGlobalVendorLoaderService(globalVendorLoaderService);
        service.setVendorDBUtil(vendorDBUtil);
        service.setParameterLoaderService(parameterLoaderService);
        VendorGoodContentExclusionConfig configMock = Mockito.mock(VendorGoodContentExclusionConfig.class);
        Mockito.when(configMock.vendorExclusionService())
            .thenReturn(Mockito.mock(VendorGoodContentExclusionService.class));
        service.setVendorGoodContentExclusionConfig(configMock);
        nameFilter.setName(NAME.getWord());

        NamedParameterJdbcTemplate contentJdbc = Mockito.mock(NamedParameterJdbcTemplate.class);
        JdbcOperations jdbcOperations = Mockito.mock(JdbcOperations.class);
        when(contentJdbc.getJdbcOperations()).thenReturn(jdbcOperations);
        ReflectionTestUtils.setField(service, "contentJdbc", contentJdbc);
        ReflectionTestUtils.setField(globalVendorLoaderService, "contentJdbc", contentJdbc);
    }

    private void updateWordsIds(List<Word> words, long id) {
        if (!CollectionUtils.isEmpty(words)) {
            for (Word word : words) {
                word.setId(id);
            }
        }
    }

    @Test
    public void emptyAliasVendorCreated() {
        // arrange
        GlobalVendor vendor = new GlobalVendor();
        vendor.setNames(Collections.singletonList(NAME));
        vendor.setAliases(Arrays.asList(ALIAS1, EMPTY_ALIAS, ALIAS2, NULL_ALIAS));

        // act
        service.createVendor(vendor, TEST_UID);

        // assert
        List<Word> writtenAliases = vendorDb.getGlobalVendors(nameFilter, 0, 1).get(0).getAliases();
        Assert.assertEquals(Arrays.asList(ALIAS1, ALIAS2), writtenAliases);
    }

    @Test
    public void emptyAliasVendorUpdated() {
        // arrange
        GlobalVendor vendor = new GlobalVendor();
        vendor.setNames(Collections.singletonList(NAME));
        vendor.setAliases(Arrays.asList(ALIAS2, ALIAS1));
        vendorDb.createVendor(vendor, TEST_UID);
        vendor.setAliases(Arrays.asList(ALIAS1, EMPTY_ALIAS, ALIAS2, NULL_ALIAS));

        // act
        service.updateVendor(vendor, TEST_UID, false, true, false, false);

        // assert
        GlobalVendor writtenVendor = vendorDb.getGlobalVendors(nameFilter, 0, 1).get(0);
        List<Word> writtenAliases = writtenVendor.getAliases();
        List<Word> expectedAliases = Arrays.asList(ALIAS1, ALIAS2);
        updateWordsIds(expectedAliases, writtenVendor.getId());
        Assert.assertEquals(expectedAliases, writtenAliases);
    }

    /**
     * MBO-24721.
     */
    @Test
    public void removeVendorLinesExistingThrowsOperationException() {
        // Returning list with any OverridenOption, simulating existing line
        IParameterLoaderService parameterLoaderServiceMock = Mockito.mock(IParameterLoaderService.class);
        doReturn(Lists.newArrayList(new OptionImpl(Option.OptionType.LINE))).when(parameterLoaderServiceMock)
            .getLines(eq(TEST_UID));
        service.setParameterLoaderService(parameterLoaderServiceMock);
        globalVendorLoaderService.setParameterLoaderService(parameterLoaderServiceMock);

        doReturn(Collections.emptyList()).when(linkServiceInterfaceMock).listLinksForVendor(anyLong());
        service.setValueLinkService(linkServiceInterfaceMock);
        globalVendorLoaderService.setValueLinkService(linkServiceInterfaceMock);

        ReflectionTestUtils.setField(service, "contentJdbc", createNamedParameterJdbcTemplate());
        ReflectionTestUtils.setField(service, "sizeMeasureService", Mockito.mock(SizeMeasureService.class));
        ReflectionTestUtils.setField(service, "kdNoAudit", Mockito.mock(KnowledgeDepotService.class));
        ReflectionTestUtils.setField(globalVendorLoaderService, "contentJdbc", createNamedParameterJdbcTemplate());
        ReflectionTestUtils.setField(globalVendorLoaderService, "kdNoAudit", Mockito.mock(KnowledgeDepotService.class));

        exception.expect(OperationException.class);
        exception.expectMessage("Вендор 322 имеет привязанные линейки");
        service.removeVendor(TEST_UID, TEST_UID);
    }

    /**
     * Shouldn't throw nothing, checking normal flow with no lines.
     * MBO-24721.
     */
    @Test
    public void removeVendorLinesNotExisting() {
        IParameterLoaderService parameterLoaderServiceMock = Mockito.mock(IParameterLoaderService.class);
        doReturn(Lists.emptyList()).when(parameterLoaderServiceMock).getLines(eq(TEST_UID));
        service.setParameterLoaderService(parameterLoaderServiceMock);
        globalVendorLoaderService.setParameterLoaderService(parameterLoaderServiceMock);

        doReturn(Collections.emptyList()).when(linkServiceInterfaceMock).listLinksForVendor(anyLong());


        ReflectionTestUtils.setField(service, "contentJdbc", createNamedParameterJdbcTemplate());
        ReflectionTestUtils.setField(service, "sizeMeasureService", Mockito.mock(SizeMeasureService.class));
        ReflectionTestUtils.setField(service, "kdNoAudit", Mockito.mock(KnowledgeDepotService.class));
        ReflectionTestUtils.setField(globalVendorLoaderService, "contentJdbc", createNamedParameterJdbcTemplate());
        ReflectionTestUtils.setField(globalVendorLoaderService, "kdNoAudit", Mockito.mock(KnowledgeDepotService.class));
        service.removeVendor(TEST_UID, TEST_UID);
    }

    @Test
    public void loadManufacturerTest() {
        GlobalVendor vendor = new GlobalVendor();

        OptionImpl manufacturer = new OptionImpl();
        final long manufacturerId = 1L;
        manufacturer.setNames(Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, "name")));
        manufacturer.setId(manufacturerId);

        vendor.setManufacturer(manufacturer);
        vendor.setNames(Collections.singletonList(NAME));

        vendorDb.createVendor(vendor, TEST_UID);

        service.loadExtendedGlobalVendor(vendor.getId());

        verify(parameterLoaderService, times(1)).getAllManufactures();
    }

    @Test
    public void setNewManufacturerTest() {
        GlobalVendor vendor = new GlobalVendor();

        OptionImpl manufacturer = new OptionImpl();
        final long manufacturerId = 1L;
        manufacturer.setNames(Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, "name")));
        manufacturer.setId(manufacturerId);

        vendor.setManufacturer(manufacturer);
        vendor.setNames(Collections.singletonList(NAME));

        vendorDb.createVendor(vendor, TEST_UID);

        OptionImpl newManufacturer = new OptionImpl();
        final long newManufacturerId = 2L;
        newManufacturer.setNames(Collections.singletonList(new Word(Word.DEFAULT_LANG_ID, "name")));
        newManufacturer.setId(newManufacturerId);

        GlobalVendor vendorForUpdate = vendor.copy();
        vendorForUpdate.setNames(Collections.singletonList(NAME));
        vendorForUpdate.setManufacturer(newManufacturer);

        service.updateVendor(vendorForUpdate, TEST_UID, false, true, false, false);

        Filter vendorFilter = new Filter();
        vendorFilter.setId(vendor.getId());
        GlobalVendor writtenVendor = vendorDb.getGlobalVendors(vendorFilter, 0, 1).get(0);

        ValueLink newValueLink = new ValueLink();
        newValueLink.setType(ValueLinkType.MANUFACTURER);
        newValueLink.setLinkDirection(LinkDirection.DIRECT);
        newValueLink.setSourceParamId(KnownIds.VENDOR_PARAM_ID);
        newValueLink.setSourceOptionId(vendor.getId());
        newValueLink.setTargetParamId(KnownIds.MANUFACTURER_PARAM_ID);
        newValueLink.setTargetOptionId(newManufacturerId);

        ValueLinkSearchCriteria criteriaForRemove = new ValueLinkSearchCriteria();
        criteriaForRemove.setType(ValueLinkType.MANUFACTURER);
        criteriaForRemove.setSourceOptionIds(vendor.getId());

        Assert.assertEquals(newManufacturer, writtenVendor.getManufacturer());

        verify(linkServiceInterfaceMock, times(1)).saveValueLink(newValueLink);
        verify(linkServiceInterfaceMock, times(1)).removeValueLinks(criteriaForRemove);
    }

    @Nonnull
    private NamedParameterJdbcTemplate createNamedParameterJdbcTemplate() {
        JdbcOperations jdbcOperations = Mockito.mock(JdbcOperations.class);
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        Mockito.when(namedParameterJdbcTemplate.getJdbcOperations()).thenReturn(jdbcOperations);
        return namedParameterJdbcTemplate;
    }
}
