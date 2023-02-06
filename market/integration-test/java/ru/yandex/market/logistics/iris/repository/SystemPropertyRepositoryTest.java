package ru.yandex.market.logistics.iris.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.logistics.iris.service.system.SystemPropertySequenceKey.LOGBROKER_PUSH_SEQ_NO;

public class SystemPropertyRepositoryTest extends AbstractContextualTest {

    @Autowired
    private SystemPropertyRepository systemPropertyRepository;

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/system_property/1.xml")
    public void shouldGetIntegerPropertyValue() {
        Integer expectedValue = systemPropertyRepository.getIntegerProperty("TEST_PROPERTY");

        assertSoftly(assertions -> {
            assertions.assertThat(expectedValue).isNotNull();
            assertions.assertThat(expectedValue).isEqualTo(10);
        });
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/system_property/1.xml")
    public void shouldReturnNullIfPropertyAreNotPresent() {
        Integer expectedValue = systemPropertyRepository.getIntegerProperty("TEST_PROPERTY_IS_NOT_PRESENT");

        assertSoftly(assertions -> assertions.assertThat(expectedValue).isNull());
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/system_property/2.xml")
    public void shouldGetStringPropertyValue() {
        String expectedValue = systemPropertyRepository.getStringProperty("TEST_PROPERTY");

        assertSoftly(assertions -> {
            assertions.assertThat(expectedValue).isNotNull();
            assertions.assertThat(expectedValue).isEqualTo("TEST_VALUE");
        });
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/system_property/2.xml")
    public void shouldReturnNullIfStringPropertyAreNotPresent() {
        String expectedValue = systemPropertyRepository.getStringProperty("TEST_PROPERTY_IS_NOT_PRESENT");

        assertSoftly(assertions -> assertions.assertThat(expectedValue).isNull());
    }

    @Test
    public void incrementAndGetPropertyValueTest() {
        Long incrementValue = systemPropertyRepository.incrementAndGetSequence(LOGBROKER_PUSH_SEQ_NO.name());
        assertThat(incrementValue, equalTo(1L));
        incrementValue = systemPropertyRepository.incrementAndGetSequence(LOGBROKER_PUSH_SEQ_NO.name());
        assertThat(incrementValue, equalTo(2L));
    }
}
