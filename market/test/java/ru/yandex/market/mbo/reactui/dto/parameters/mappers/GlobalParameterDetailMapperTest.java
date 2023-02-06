package ru.yandex.market.mbo.reactui.dto.parameters.mappers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.tools.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StreamUtils;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.InheritedParameter;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.params.ParameterOverride;
import ru.yandex.market.mbo.gwt.models.params.SubType;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.reactui.dto.parameters.GlobalParameterDetails;
import ru.yandex.market.mbo.reactui.dto.parameters.ParamOptionDto;
import ru.yandex.market.mbo.reactui.dto.parameters.WordDto;
import ru.yandex.market.mbo.reactui.mapper.WordMapper;
import ru.yandex.market.mbo.reactui.service.ParameterServiceRemote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mbo.gwt.models.param.ParamUtil.GLOBAL_PARAMETERS_HID;

@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(MockitoJUnitRunner.class)
public class GlobalParameterDetailMapperTest {
    @Mock
    private ParameterServiceRemote parameterService;

    @InjectMocks
    private GlobalParameterDetailMapper mapper = Mappers.getMapper(GlobalParameterDetailMapper.class);

    private CategoryParam createTestParam() {
        CategoryParam testParam = new Parameter();
        testParam.setNames(Stream.of("test1") //на каждом языке может быть только одно имя
            .map(WordUtil::defaultWord)
            .collect(Collectors.toList()));
        testParam.setType(Param.Type.NUMERIC);
        testParam.setSubtype(SubType.NOT_DEFINED);
        return testParam;
    }

    @Before
    public void setUp() {
        Mockito.when(parameterService.loadParameter(Mockito.eq(GLOBAL_PARAMETERS_HID), Mockito.longThat(a -> a > 0)))
            .thenReturn(createTestParam());
        WordDtoMapper wordDtoMapper = Mappers.getMapper(WordDtoMapper.class);
        ReflectionTestUtils.setField(mapper, "wordDtoMapper", wordDtoMapper);
        ParamOptionDtoMapper paramOptionDtoMapper = Mappers.getMapper(ParamOptionDtoMapper.class);
        ReflectionTestUtils.setField(paramOptionDtoMapper, "wordDtoMapper", wordDtoMapper);
        ReflectionTestUtils.setField(paramOptionDtoMapper,
            "paramOptionNameMapper",
            Mappers.getMapper(ParamOptionDtoMapper.ParamOptionNameMapper.class));
        ReflectionTestUtils.setField(paramOptionDtoMapper,
            "vendorInfoMapper",
            Mappers.getMapper(ParamOptionDtoMapper.VendorInfoMapper.class));
    }

    @Test
    public void getParameterType() {
        CategoryParam color = new Parameter();
        color.setSubtype(SubType.COLOR);
        assertEquals(GlobalParameterDetails.ParameterType.COLOR, mapper.getParameterType(color));

        CategoryParam vendorLine = new Parameter();
        vendorLine.setXslName(XslNames.VENDOR_LINE);
        assertEquals(GlobalParameterDetails.ParameterType.LINE, mapper.getParameterType(vendorLine));

        CategoryParam managedExternally = new Parameter();
        managedExternally.setManagedExternally(true);
        assertEquals(GlobalParameterDetails.ParameterType.MANAGED_EXTERNALLY,
            mapper.getParameterType(managedExternally));

        CategoryParam imagePicker = new Parameter();
        imagePicker.setType(Param.Type.ENUM);
        imagePicker.setSubtype(SubType.IMAGE_PICKER);
        assertEquals(GlobalParameterDetails.ParameterType.IMAGE_PICKER, mapper.getParameterType(imagePicker));

        CategoryParam bool = new Parameter();
        bool.setType(Param.Type.BOOLEAN);
        assertEquals(GlobalParameterDetails.ParameterType.BOOLEAN, mapper.getParameterType(bool));

        CategoryParam defaultParam = new Parameter();
        assertEquals(GlobalParameterDetails.ParameterType.DEFAULT, mapper.getParameterType(defaultParam));
    }

    @Test
    public void getManualInheritance() {
        CategoryParam parent = new Parameter();
        parent.setId(1L);

        InheritedParameter inheritedParameter = new InheritedParameter(parent);
        inheritedParameter.setManualInheritance(true);
        assertTrue(mapper.getManualInheritance(inheritedParameter));

        ParameterOverride parameterOverride = new ParameterOverride();
        parameterOverride.setManualInheritance(true);
        assertTrue(mapper.getManualInheritance(parameterOverride));

        Parameter casualParam = new Parameter();
        assertFalse(mapper.getManualInheritance(casualParam));
    }

    @Test
    public void testNewParamMapping() {
        GlobalParameterDetails details = new GlobalParameterDetails();
        details.setId(0L);
        details.setNames(
            Stream.of("new1") //на каждом языке может быть только одно имя
                .map(WordUtil::defaultWord)
                .map(WordMapper::of)
                .collect(Collectors.toList())
        );
        details.setAliases(
            Stream.of("newAlias1", "newAlias2")
                .map(WordUtil::defaultWord)
                .map(WordMapper::of)
                .collect(Collectors.toList())
        );
        details.setType(Param.Type.NUMERIC);
        details.setSubtype(SubType.NOT_DEFINED);

        CategoryParam parameter = mapper.toParameter(details);

        assertAliases(details.getNames(), parameter.getNames());
        assertAliases(details.getAliases(), parameter.getLocalizedAliases());
        assertEquals(details.getId().longValue(), parameter.getId());
    }

    @Test
    public void testParamMappingNames() {
        GlobalParameterDetails details = new GlobalParameterDetails();
        details.setId(101L);
        details.setNames(
            Stream.of("abc") //на каждом языке может быть только одно имя
                .map(WordUtil::defaultWord)
                .map(WordMapper::of)
                .collect(Collectors.toList())
        );
        details.setAliases(
            Stream.of("ghi", "jkl")
                .map(WordUtil::defaultWord)
                .map(WordMapper::of)
                .collect(Collectors.toList())
        );
        details.setType(Param.Type.NUMERIC);
        details.setSubtype(SubType.NOT_DEFINED);

        CategoryParam parameter = mapper.toParameter(details);

        assertAliases(details.getNames(), parameter.getNames());
        assertAliases(details.getAliases(), parameter.getLocalizedAliases());
        assertEquals(details.getId().longValue(), parameter.getId());
    }


    // TODO fix it
    @Test
    @Ignore
    public void testGlobal() throws IOException {
        assertGlobalMapping("mappers/global/global-parameter-details-to-update.json");
        assertGlobalMapping("mappers/global/global-parameter-details-to-save.json");
        assertGlobalMapping("mappers/global/global-parameter-details-to-save-2.json");
    }

    private void assertGlobalMapping(String globalDetailsPath) throws IOException {
        ClassPathResource globalParamDetails =
            new ClassPathResource(globalDetailsPath);
        ObjectMapper om = new ObjectMapper();
        StreamUtils.copyToString(globalParamDetails.getInputStream(), StandardCharsets.UTF_8);
        GlobalParameterDetails details = om
            .readValue(globalParamDetails.getInputStream(), GlobalParameterDetails.class);

        CategoryParam parameter = mapper.toParameter(details);
        assertParameter(details, parameter);
    }

    private void assertParameter(GlobalParameterDetails details, CategoryParam parameter) {
        assertEquals(details.getDescriptionForDictionary(), parameter.getDescription());
        assertEquals(details.getFormalizerTagId() != null
            ? details.getFormalizerTagId() : 0, parameter.getFormalizerTag().getId());
        assertAliases(details.getAliases(), parameter.getLocalizedAliases());
        assertParamType(details.getParameterType(), parameter);
        assertEquals(details.getService(), parameter.isService());
        assertEquals(details.getCommentForOperator(), parameter.getCommentForOperator());
        assertEquals(details.getCommentForPartner(), parameter.getCommentForPartner());
        assertEquals(details.getHidden(), parameter.isHidden());
        assertEquals(details.getAdvFilterIndex(), parameter.getAdvFilterIndex());
        assertEquals(details.getCommonFilterIndex(), parameter.getCommonFilterIndex());
        assertEquals(details.getModelFilterIndex(), parameter.getModelFilterIndex());
        assertEquals(details.getId().longValue(), parameter.getId());
        assertAliases(details.getNames(), parameter.getNames());
        assertEquals(details.getSubtype(), parameter.getSubtype());
        assertEquals(details.getXslName(), parameter.getXslName());
        assertEquals(details.getUseForGuru(), parameter.isUseForGuru());
        assertEquals(details.getUseForGurulight(), parameter.isUseForGurulight());
        assertEquals(details.getRealParamId() != null
            ? details.getRealParamId() : 0L, parameter.getRealParamId());
        assertEquals(details.getDontUseAsAlias(), parameter.isDontUseAsAlias());
        assertEquals(details.getLevel(), parameter.getLevel());
        assertEquals(details.getMeasureId(), parameter.getMeasureId());
        assertEquals(details.getUnitId(), parameter.getUnitId());
        assertEquals(details.getShortEnumCount(), parameter.getShortEnumCount());
        assertEquals(details.getShortEnumSortType(), parameter.getShortEnumSortType());
        assertEquals(details.getMdmParameter(), parameter.isMdmParameter());
        assertEquals(details.getLocalValueInheritanceStrategy(), parameter.getLocalValueInheritanceStrategy());
        assertEquals(details.getMandatoryForPartner(), parameter.isMandatoryForPartner());
    }

    private void assertParamType(GlobalParameterDetails.ParameterType parameterType, CategoryParam parameter) {
        if (parameterType == GlobalParameterDetails.ParameterType.COLOR) {
            assertEquals(SubType.COLOR, parameter.getSubtype());
        } else if (parameterType == GlobalParameterDetails.ParameterType.IMAGE_PICKER) {
            assertTrue(parameter.isEnum());
            assertEquals(SubType.IMAGE_PICKER, parameter.getSubtype());
        }
    }

    private void assertOptions(List<ParamOptionDto> paramOptions, List<Option> options) {
        assertEquals(CollectionUtils.isEmpty(paramOptions), CollectionUtils.isEmpty(options));
        if (CollectionUtils.isEmpty(paramOptions) && CollectionUtils.isEmpty(options)) {
            return;
        }
        paramOptions.forEach(p -> assertOption(p, options));
    }

    private void assertOption(ParamOptionDto paramOption, List<Option> options) {
        Option option = options.stream()
            .filter(o -> o.getValueId() == paramOption.getValueId())
            .findFirst()
            .orElse(null);
        assertNotNull(option);
        assertEquals(paramOption.getValueId().longValue(), option.getValueId());
        assertEquals(paramOption.getTagCode().getTag(), option.getTag());
        assertEquals(paramOption.getTagCode().getCode(), option.getCode());
        assertEquals(paramOption.getDisplayName(), option.getDisplayName());
        assertEquals(paramOption.getPublished(), option.isPublished());
        assertEquals(paramOption.getActive(), option.isActive());
        assertAliases(paramOption.getAliases(), option.getLocalizedAliases());
        assertEquals(paramOption.getParamOptionName().getName(), option.getName());
    }

    private void assertAliases(List<WordDto> wordDtos, List<? extends Word> words) {
        wordDtos.forEach(w -> assertWords(w, words));
    }

    private void assertWords(WordDto wordDto, List<? extends Word> words) {
        Word word = words.stream()
            .filter(w -> StringUtils.equals(w.getWord(), wordDto.getWord()))
            .findFirst()
            .orElse(null);
        assertNotNull(word);
        assertEquals(wordDto.getWord(), word.getWord());
        assertEquals(wordDto.getLanguage().getId(), word.getLangId());
    }
}
