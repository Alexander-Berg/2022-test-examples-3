package ru.yandex.market.mbi.api.controller.outlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.outlet.OutletLicenseType;
import ru.yandex.market.mbi.api.client.entity.outlets.license.OutletLicenseDTO;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Тест проверки сериализации / десериализации {@link OutletLicenseDTO}.
 *
 * @author Vladislav Bauer
 */
class OutletLicenseTypeSerializationTest extends FunctionalTest {

    @Autowired
    private ObjectMapper mbiApiObjectMapper;


    @Test
    @DisplayName("Проверить возможность десериализации неизвестного типа лицензий")
    void testDeserializationUnknownLicenseType() throws Exception {
        checkDeserialization("I_AM_BAD_LICENSE_TYPE", OutletLicenseType.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(OutletLicenseType.class)
    @DisplayName("Проверить десериализацию известных типов лицензий")
    void testDeserializationKnownLicenseType(final OutletLicenseType type) throws Exception {
        checkDeserialization(type.name(), type);
    }


    private void checkDeserialization(final String typeName, final OutletLicenseType expectedType) throws Exception {
        final String xmlData = String.format("<outletLicense type=\"%s\"></outletLicense>", typeName);
        final OutletLicenseDTO dto = mbiApiObjectMapper.readValue(xmlData, OutletLicenseDTO.class);

        assertThat(dto.getType(), equalTo(expectedType));
    }

}
