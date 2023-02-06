package ru.yandex.market.common.mds.s3.spring.db;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.junit.Test;

import ru.yandex.market.common.mds.s3.spring.model.ResourceConfigurationStatus;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Unit-тест для {@link ResourceConfigurationStatus}.
 *
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
public class ResourceConfigurationStatusTest {

    @Test
    public void testValuesCorrectness() {
        final Collection<ResourceConfigurationStatus> values = Arrays.asList(ResourceConfigurationStatus.values());
        final Collection<Integer> uniqueCodes = values.stream()
                .map(ResourceConfigurationStatus::getCode)
                .collect(Collectors.toSet());

        assertThat(uniqueCodes.size(), is(values.size()));
    }

}
