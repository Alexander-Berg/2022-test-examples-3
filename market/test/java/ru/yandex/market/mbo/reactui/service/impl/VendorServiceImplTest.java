package ru.yandex.market.mbo.reactui.service.impl;

import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.params.guru.GuruVendorsReader;
import ru.yandex.market.mbo.db.vendor.GlobalVendorService;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;

import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.reactui.dto.vendor.SearchVendorRequest;
import ru.yandex.market.mbo.reactui.dto.vendor.SearchVendorResponse;
import ru.yandex.market.mbo.reactui.dto.vendor.VendorDto;
import ru.yandex.market.mbo.reactui.util.VendorDtoConverter;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VendorServiceImplTest {

    @Mock
    private GuruVendorsReader guruVendorsReader;
    @Mock
    private GlobalVendorService globalVendorService;
    @InjectMocks
    private VendorServiceImpl vendorService;

    private static final int LANG_ID = 225;

    @Test
    @DisplayName("Without query parameter")
    public void simpleSearchTest() {
        final long categoryId = 1L;

        SearchVendorRequest request = new SearchVendorRequest();
        request.setCategoryId(categoryId);
        request.setOffset(1);
        request.setLimit(1);

        OptionImpl localVendor1 = generateLocalVendor("a");
        OptionImpl localVendor2 = generateLocalVendor("b");
        OptionImpl localVendor3 = generateLocalVendor("c");

        List<OptionImpl> localVendors = List.of(
            localVendor3,
            localVendor1,
            localVendor2
        );
        List<VendorDto> expectedResult = Stream.of(localVendor2)
            .map(e -> VendorDtoConverter.convert(e, categoryId))
            .collect(Collectors.toList());

        when(guruVendorsReader.getVendorsByHid(categoryId)).thenReturn(localVendors);

        SearchVendorResponse result = vendorService.searchVendors(request);

        assertEquals(localVendors.size(), result.getTotal());
        assertEquals(expectedResult.size(), result.getSize());
        assertEquals(expectedResult, result.getVendors());
    }

    @Test
    @DisplayName("Without query and category parameters")
    public void simpleSearchTestWithoutCategoryId() {
        SearchVendorRequest request = new SearchVendorRequest();
        request.setOffset(0);
        request.setLimit(100);

        SearchVendorResponse result = vendorService.searchVendors(request);

        assertEquals(0, result.getSize());
        assertEquals(0, result.getTotal());
        assertTrue(result.getVendors().isEmpty());
    }

    @Test
    public void searchForSuggestWithoutCategoryId() {
        SearchVendorRequest request = new SearchVendorRequest();
        request.setOffset(1);
        request.setLimit(2);
        request.setQuery("a");

        GlobalVendor globalVendor1 = generateGlobalVendor("abb");
        GlobalVendor globalVendor2 = generateGlobalVendor("bab");
        GlobalVendor globalVendor3 = generateGlobalVendor("bba");

        List<GlobalVendor> globalVendors = List.of(globalVendor1, globalVendor2, globalVendor3);
        List<VendorDto> expectedResult = Stream.of(globalVendor1, globalVendor2, globalVendor3)
            .map(VendorDtoConverter::convert)
            .collect(Collectors.toList());

        when(globalVendorService.getGlobalVendors(any(), eq(request.getOffset()), eq(request.getLimit())))
            .thenReturn(globalVendors);
        when(globalVendorService.getGlobalVendorsCount(any())).thenReturn(globalVendors.size());

        SearchVendorResponse result = vendorService.searchVendors(request);

        assertEquals(expectedResult.size(), result.getTotal());
        assertEquals(expectedResult.size(), result.getSize());
        assertEquals(expectedResult, result.getVendors());
    }

    @Test
    public void searchForSuggestByCategoryId() {
        final long categoryId = 1L;

        SearchVendorRequest request = new SearchVendorRequest();
        request.setCategoryId(categoryId);
        request.setOffset(1);
        request.setLimit(2);
        request.setOnlyLocal(true);
        request.setQuery("a");

        OptionImpl localVendor1 = generateLocalVendor("bab");
        OptionImpl localVendor2 = generateLocalVendor("abb");
        OptionImpl localVendor3 = generateLocalVendor("bba");
        OptionImpl localVendor4 = generateLocalVendor("bbb");

        List<OptionImpl> localVendors = List.of(
            localVendor3,
            localVendor1,
            localVendor2,
            localVendor4
        );
        List<VendorDto> expectedResult = Stream.of(localVendor1, localVendor3)
            .map(e -> VendorDtoConverter.convert(e, categoryId))
            .collect(Collectors.toList());

        when(guruVendorsReader.getVendorsByHid(categoryId)).thenReturn(localVendors);

        SearchVendorResponse result = vendorService.searchVendors(request);

        final int expectedTotal = 3;
        assertEquals(expectedTotal, result.getTotal());
        assertEquals(expectedResult.size(), result.getSize());
        assertEquals(expectedResult, result.getVendors());
    }


    private OptionImpl generateLocalVendor(String name) {
        OptionImpl localVendor = new OptionImpl();
        Option globalVendor = new OptionImpl();

        globalVendor.setNames(List.of(new Word(LANG_ID, name)));
        localVendor.setParent(globalVendor);

        return localVendor;
    }

    private GlobalVendor generateGlobalVendor(String name) {
        GlobalVendor globalVendor = new GlobalVendor();
        globalVendor.setNames(List.of(new Word(LANG_ID, name)));
        return globalVendor;
    }

}
