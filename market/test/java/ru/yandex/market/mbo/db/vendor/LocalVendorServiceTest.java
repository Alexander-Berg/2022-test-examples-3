package ru.yandex.market.mbo.db.vendor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.db.GuruVendorsReaderStub;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.params.ParameterSaveContext;
import ru.yandex.market.mbo.db.params.ParameterService;
import ru.yandex.market.mbo.gwt.models.params.InheritedParameter;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;
import ru.yandex.market.mbo.gwt.utils.WordUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.getGlobalVendor;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.getInheritedVendorParam;
import static ru.yandex.market.mbo.db.utils.ParameterTestHelper.getLocalVendor;

/**
 * @author danfertev
 * @since 18.06.2019
 */
public class LocalVendorServiceTest {
    private static final long CATEGORY_ID = 1L;
    private static final long GLOBAL_VENDOR_ID = 11L;
    private static final long UID = 100500L;

    private LocalVendorService localVendorService;
    private ParameterLoaderServiceStub parameterLoaderServiceStub;
    private GuruVendorsReaderStub guruVendorsReaderStub;
    private GlobalVendorServiceMock globalVendorServiceMock;
    private ParameterService parameterServiceMock;

    @Before
    public void setUp() {
        parameterLoaderServiceStub = new ParameterLoaderServiceStub();
        guruVendorsReaderStub = new GuruVendorsReaderStub();
        globalVendorServiceMock = new GlobalVendorServiceMock();
        parameterServiceMock = mock(ParameterService.class);
        when(parameterServiceMock.createDefaultSaveContext(anyLong())).then(args -> {
            long uid = args.getArgument(0);
            return new ParameterSaveContext(uid);
        });

        localVendorService = new LocalVendorService(parameterLoaderServiceStub, guruVendorsReaderStub,
            globalVendorServiceMock, parameterServiceMock);
    }

    @Test(expected = RuntimeException.class)
    public void testNoVendorParam() {
        localVendorService.addLocalVendor(CATEGORY_ID, GLOBAL_VENDOR_ID, UID);
    }

    @Test
    public void testLocalVendorExist() {
        InheritedParameter vendorParam = getInheritedVendorParam(CATEGORY_ID);
        GlobalVendor globalVendor = getGlobalVendor(GLOBAL_VENDOR_ID);
        OptionImpl localVendor = getLocalVendor(vendorParam, globalVendor);
        parameterLoaderServiceStub.addCategoryParam(vendorParam);
        globalVendorServiceMock.addVendor(globalVendor);
        guruVendorsReaderStub.addVendor(globalVendor.getId(), vendorParam.getCategoryHid(), localVendor);

        localVendorService.addLocalVendor(CATEGORY_ID, GLOBAL_VENDOR_ID, UID);
        verify(parameterServiceMock, never()).addLocalVendor(any(), anyLong(), any(), any());
    }

    @Test
    public void testAddLocalVendor() {
        InheritedParameter vendorParam = getInheritedVendorParam(CATEGORY_ID);
        GlobalVendor globalVendor = getGlobalVendor(GLOBAL_VENDOR_ID);
        parameterLoaderServiceStub.addCategoryParam(vendorParam);
        globalVendorServiceMock.addVendor(globalVendor);

        localVendorService.addLocalVendor(CATEGORY_ID, GLOBAL_VENDOR_ID, false, UID);

        ArgumentCaptor<ParameterSaveContext> contextCaptor = ArgumentCaptor.forClass(ParameterSaveContext.class);
        ArgumentCaptor<OptionImpl> localVendorCaptor = ArgumentCaptor.forClass(OptionImpl.class);
        verify(parameterServiceMock, times(1)).addLocalVendor(contextCaptor.capture(),
            eq(CATEGORY_ID), eq(vendorParam), localVendorCaptor.capture());

        ParameterSaveContext context = contextCaptor.getValue();
        assertThat(context.isBilledOperation()).isFalse();
        assertThat(context.getUid()).isEqualTo(UID);

        OptionImpl localVendor = localVendorCaptor.getValue();
        assertThat(localVendor.getParentVendorId()).isEqualTo(globalVendor.getId());
        assertThat(localVendor.isPublished()).isEqualTo(globalVendor.isPublished());
    }

    @Test
    public void testAddLocalNotDefinedVendor() {
        InheritedParameter vendorParam = getInheritedVendorParam(CATEGORY_ID);
        GlobalVendor globalVendor = getGlobalVendor(KnownIds.NOT_DEFINED_GLOBAL_VENDOR);
        parameterLoaderServiceStub.addCategoryParam(vendorParam);
        globalVendorServiceMock.addVendor(globalVendor);

        localVendorService.addLocalVendor(CATEGORY_ID, KnownIds.NOT_DEFINED_GLOBAL_VENDOR, false, UID);

        ArgumentCaptor<OptionImpl> localVendorCaptor = ArgumentCaptor.forClass(OptionImpl.class);
        verify(parameterServiceMock, times(1)).addLocalVendor(Mockito.any(),
            eq(CATEGORY_ID), eq(vendorParam), localVendorCaptor.capture());

        OptionImpl localVendor = localVendorCaptor.getValue();
        Assert.assertFalse(localVendor.isFilterValue());
    }

    @Test
    public void testAddLocalNotDefinedAlreadyExistsVendor() {
        InheritedParameter vendorParam = getInheritedVendorParam(CATEGORY_ID);
        GlobalVendor globalVendor = getGlobalVendor(KnownIds.NOT_DEFINED_GLOBAL_VENDOR);
        parameterLoaderServiceStub.addCategoryParam(vendorParam);
        globalVendorServiceMock.addVendor(globalVendor);

        Option option = new OptionImpl();
        option.addName(WordUtil.defaultWord(globalVendor.getDefaultName()));
        option.setId(globalVendor.getId());
        option.setPublished(globalVendor.isPublished());

        OptionImpl newLocalVendor = new OptionImpl(Option.OptionType.VENDOR);
        newLocalVendor.setParent(option);
        newLocalVendor.setPublished(globalVendor.isPublished());
        newLocalVendor.setActive(true);
        newLocalVendor.setFilterValue(true);
        guruVendorsReaderStub.addVendor(KnownIds.NOT_DEFINED_GLOBAL_VENDOR, CATEGORY_ID, newLocalVendor);

        localVendorService.addLocalVendor(CATEGORY_ID, KnownIds.NOT_DEFINED_GLOBAL_VENDOR, false, UID);

        OptionImpl localVendor = guruVendorsReaderStub.getLocalVendor(CATEGORY_ID,
            KnownIds.NOT_DEFINED_GLOBAL_VENDOR);
        Assert.assertFalse(localVendor.isFilterValue());
    }
}
