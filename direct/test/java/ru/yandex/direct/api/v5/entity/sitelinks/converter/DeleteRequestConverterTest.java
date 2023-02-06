package ru.yandex.direct.api.v5.entity.sitelinks.converter;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.yandex.direct.api.v5.general.IdsCriteria;
import com.yandex.direct.api.v5.sitelinks.DeleteRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DeleteRequestConverterTest {

    private DeleteRequestConverter converter = new DeleteRequestConverter();

    @Parameter()
    public DeleteRequest request;

    @Parameter(1)
    public List<Long> expectedResult;

    @Parameters
    public static Iterable<Object[]> parameters() {
        return Arrays.asList(

                // negative cases: all handled silently
                requestAndExpected(null, emptyList()),          // null ids
                requestAndExpected(new Long[]{}),               // empty ids
                requestAndExpected(0L, null),                   // ids collection contains null value

                // positive
                requestAndExpected(0L),
                requestAndExpected(1L, 1000L, -30L, 1L)
        );
    }

    private static Object[] requestAndExpected(@Nonnull Long... ids) {
        List<Long> expected = Arrays.asList(ids);
        return new Object[]{requestFrom(ids), expected};
    }

    private static Object[] requestAndExpected(@Nullable Long[] ids, List<Long> expected) {
        return new Object[]{requestFrom(ids), expected};
    }

    private static DeleteRequest requestFrom(Long... ids) {
        return new DeleteRequest().withSelectionCriteria(new IdsCriteria().withIds(ids));
    }

    @Test
    public void shouldReturnSelectionContainingAllIdsFromRequest() {
        assertThat(converter.convert(request)).containsExactlyInAnyOrderElementsOf(expectedResult);
    }

}

