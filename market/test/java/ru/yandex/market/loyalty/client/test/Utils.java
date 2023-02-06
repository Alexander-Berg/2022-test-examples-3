package ru.yandex.market.loyalty.client.test;

import uk.co.jemos.podam.api.AttributeMetadata;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;
import uk.co.jemos.podam.api.PodamUtils;
import uk.co.jemos.podam.typeManufacturers.IntTypeManufacturerImpl;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
public class Utils {
    public static PodamFactory createPodamFactory() {
        PodamFactory podamFactory = new PodamFactoryImpl();
        podamFactory.getStrategy().addOrReplaceTypeManufacturer(int.class, new CustomIntTypeManufacturerImpl());
        return podamFactory;
    }

    private static class CustomIntTypeManufacturerImpl extends IntTypeManufacturerImpl {
        @Override
        public Integer getInteger(AttributeMetadata attributeMetadata) {
            if (attributeMetadata.getAttributeType() == Timestamp.class ||
                    attributeMetadata.getAttributeType() == Instant.class) {
                return PodamUtils.getIntegerInRange(0, 999_999_999);
            }
            return super.getInteger(attributeMetadata);
        }
    }
}
