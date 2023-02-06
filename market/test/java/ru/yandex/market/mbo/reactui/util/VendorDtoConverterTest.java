package ru.yandex.market.mbo.reactui.util;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.params.EnumAlias;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.reactui.dto.vendor.VendorDto;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class VendorDtoConverterTest {

    private static final Long VENDOR_ID = 1L;
    private static final String VENDOR_NAME = "vendor_name";
    private static final Long CATEGORY_ID = 2L;
    private static final List<Word> NAMES = List.of(
        new Word(225, VENDOR_NAME)
    );
    private static final List<Word> CUT_OFF_WORDS = List.of(
        new Word(2, "cut_off_word")
    );
    private static final Long LOCAL_VENDOR_ID = 3L;
    private static final String URL = "url";
    private static final String COMMENT = "comment";
    private static final List<EnumAlias> ALIASES = List.of(
        new EnumAlias(1L, 1, "alias", EnumAlias.ExtractionType.FOR_BOTH, EnumAlias.Type.GENERAL)
    );

    @Test
    public void convertOverrideOptionToDtoTest() {
        OptionImpl localVendor = prepareLocalVendor();
        VendorDto dto = VendorDtoConverter.convert(localVendor, CATEGORY_ID);

        assertEquals(VENDOR_ID, dto.getVendorId());
        assertEquals(VENDOR_NAME, dto.getName());
        assertEquals(Boolean.TRUE, dto.isAddedToCategory());
        assertEquals(NAMES, dto.getNames());
        assertEquals(CUT_OFF_WORDS, dto.getCutOffWords());
        assertEquals(LOCAL_VENDOR_ID, dto.getLocalVendorId());
        assertEquals(CATEGORY_ID, dto.getCategoryId());
        assertEquals(URL, dto.getUrl());
        assertEquals(COMMENT, dto.getComment());
        assertEquals(ALIASES, dto.getAliases());
    }

    @Test
    public void convertGlobalVendorToDtoTest() {
        GlobalVendor globalVendor = prepareGlobalVendor();
        VendorDto dto = VendorDtoConverter.convert(globalVendor);

        assertEquals(VENDOR_ID, dto.getVendorId());
        assertEquals(VENDOR_NAME, dto.getName());
        assertEquals(Boolean.FALSE, dto.isAddedToCategory());
        assertEquals(NAMES, dto.getNames());
        assertNull(dto.getCutOffWords());
        assertNull(dto.getLocalVendorId());
        assertNull(dto.getCategoryId());
        assertEquals(URL, dto.getUrl());
        assertEquals(COMMENT, dto.getComment());
        assertEquals(ALIASES, dto.getAliases());
    }

    private OptionImpl prepareLocalVendor() {
        final OptionImpl localVendor = new OptionImpl();

        final Option globalVendor = new OptionImpl();
        globalVendor.setId(VENDOR_ID);
        globalVendor.setDisplayName(VENDOR_NAME);
        globalVendor.setNames(NAMES);
        globalVendor.setAliases(ALIASES);

        localVendor.setParent(globalVendor);
        localVendor.setId(LOCAL_VENDOR_ID);
        localVendor.setGlobalVendorSite(URL);
        localVendor.setGlobalVendorComment(COMMENT);
        localVendor.setCutOffWords(CUT_OFF_WORDS);

        return localVendor;
    }

    private GlobalVendor prepareGlobalVendor() {
        GlobalVendor globalVendor = new GlobalVendor();

        globalVendor.setId(VENDOR_ID);
        globalVendor.setNames(NAMES);
        globalVendor.setAliases(ALIASES.stream().map(e -> (Word) e).collect(Collectors.toList()));
        globalVendor.setSite(URL);
        globalVendor.setComment(COMMENT);

        return globalVendor;
    }
}
