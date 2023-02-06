package ru.yandex.market.sqb.service.config.converter;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.model.conf.AliasModel;
import ru.yandex.market.sqb.model.vo.AliasVO;
import ru.yandex.market.sqb.service.config.converter.common.AbstractNameModelConverterTest;
import ru.yandex.market.sqb.test.ObjectGenerationUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit-тесты для {@link AliasModelConverter}.
 *
 * @author Vladislav Bauer
 */
class AliasModelConverterTest extends AbstractNameModelConverterTest<AliasVO, AliasModel> {

    private static final String TEST_NAME = "TEST_ALIAS_NAME";
    private static final String TEST_VALUE = "test alias value";


    @Test
    void testSinglePositive() {
        final AliasModelConverter converter = createConverter();
        final AliasVO aliasVO = createObject();
        final AliasModel aliasModel = converter.convert(aliasVO);

        assertThat(aliasModel.getName(), equalTo(aliasVO.getName()));
        assertThat(aliasModel.getValue(), equalTo(aliasVO.getValue()));
    }


    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    protected Collection<String> getIllegalNames() {
        final Collection<String> forbiddenNames = generateForbiddenNames();
        return CollectionUtils.union(super.getIllegalNames(), forbiddenNames);
    }

    @Nonnull
    @Override
    protected AliasVO createObject() {
        final AliasVO aliasVO = new AliasVO();
        aliasVO.setName(TEST_NAME);
        aliasVO.setValue(TEST_VALUE);
        return aliasVO;
    }

    @Nonnull
    @Override
    protected AliasModelConverter createConverter() {
        return new AliasModelConverter();
    }


    private Collection<String> generateForbiddenNames() {
        final List<String> forbiddenNames = AliasModelConverter.FORBIDDEN_NAME_PREFIXES.stream()
                .map(prefix -> prefix + ObjectGenerationUtils.createName())
                .collect(Collectors.toList());

        final Collection<String> result = Sets.newHashSet(AliasModelConverter.FORBIDDEN_NAME_PREFIXES);
        result.addAll(forbiddenNames);
        return result;
    }

}
