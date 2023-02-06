package ru.yandex.market.mbo.mdm.common.service.category;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.filter.CategorySearchFilter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.util.I18nStringUtils;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mdm.http.MdmAttributeValue;
import ru.yandex.market.mdm.http.MdmAttributeValues;
import ru.yandex.market.mdm.http.entity.MdmSearchCondition;
import ru.yandex.market.mdm.http.entity.MdmSearchKey;

@Import(
    CategorySearchFilterFactory.class
)
public class CategorySearchFilterFactoryTest extends MdmBaseDbTestClass {

    @Autowired
    private CategorySearchFilterFactory factory;

    @Test
    public void shouldCreateFilterForNumericSearchKey() {
        // given
        List<Long> middleSideLengthMinLimitPath = List.of(34839L, 34782L);

        MdmSearchKey searchKey = MdmSearchKey.newBuilder()
            .setMdmAttributeValues(MdmAttributeValues.newBuilder()
                .setMdmAttributeId(34782L)
                .addAllMdmAttributePath(middleSideLengthMinLimitPath)
                .addValues(MdmAttributeValue.newBuilder()
                    .setNumeric("20"))
                .build())
            .setCondition(MdmSearchCondition.EQ)
            .build();

        // when
        CategorySearchFilter filter = factory.fromSearchKey(searchKey);

        // then
        Assertions.assertThat(filter.getAttributeId()).isEqualTo(412);
        Assertions.assertThat(filter.getCondition()).isEqualTo(CategorySearchFilter.SearchCondition.EQ);
        Assertions.assertThat(filter.getNumerics()).containsExactly(BigDecimal.valueOf(20));
    }

    @Test
    public void shouldCreateFilterForStringSearchKey() {
        // given
        List<Long> customsCommCodePrefixPath = List.of(34521L);

        MdmSearchKey searchKey = MdmSearchKey.newBuilder()
            .setMdmAttributeValues(MdmAttributeValues.newBuilder()
                .setMdmAttributeId(34521L)
                .addAllMdmAttributePath(customsCommCodePrefixPath)
                .addValues(MdmAttributeValue.newBuilder()
                    .setString(I18nStringUtils.fromSingleRuString("test")))
                .build())
            .setCondition(MdmSearchCondition.LIKE)
            .build();

        // when
        CategorySearchFilter filter = factory.fromSearchKey(searchKey);

        // then
        Assertions.assertThat(filter.getAttributeId()).isEqualTo(19);
        Assertions.assertThat(filter.getCondition()).isEqualTo(CategorySearchFilter.SearchCondition.LIKE);
        Assertions.assertThat(filter.getStrings()).containsExactly("test");
    }

    @Test
    public void shouldCreateFilterForEnumSearchKey() {
        // given
        List<Long> expirationDatesApplyPath = List.of(34659L);

        MdmSearchKey searchKey = MdmSearchKey.newBuilder()
            .setMdmAttributeValues(MdmAttributeValues.newBuilder()
                .setMdmAttributeId(34659L)
                .addAllMdmAttributePath(expirationDatesApplyPath)
                .addValues(MdmAttributeValue.newBuilder()
                    .setOption(34699L)) // bmdmId 34699 mapped to 2 in old mdm
                .build())
            .setCondition(MdmSearchCondition.NE)
            .build();

        // when
        CategorySearchFilter filter = factory.fromSearchKey(searchKey);

        // then
        Assertions.assertThat(filter.getAttributeId()).isEqualTo(50);
        Assertions.assertThat(filter.getCondition()).isEqualTo(CategorySearchFilter.SearchCondition.NE);
        Assertions.assertThat(filter.getOptionIds()).containsExactly(2L);
    }

    @Test
    public void shouldCreateFilterForBoolSearchKey() {
        // given
        List<Long> mdmSerialNumberMaskPath = List.of(34557L);

        MdmSearchKey searchKey = MdmSearchKey.newBuilder()
            .setMdmAttributeValues(MdmAttributeValues.newBuilder()
                .setMdmAttributeId(34557L)
                .addAllMdmAttributePath(mdmSerialNumberMaskPath)
                .addValues(MdmAttributeValue.newBuilder()
                    .setBool(true))
                .build())
            .setCondition(MdmSearchCondition.EQ)
            .build();

        // when
        CategorySearchFilter filter = factory.fromSearchKey(searchKey);

        // then
        Assertions.assertThat(filter.getAttributeId()).isEqualTo(602);
        Assertions.assertThat(filter.getCondition()).isEqualTo(CategorySearchFilter.SearchCondition.EQ);
        Assertions.assertThat(filter.getBools()).containsExactly(true);
    }

}
