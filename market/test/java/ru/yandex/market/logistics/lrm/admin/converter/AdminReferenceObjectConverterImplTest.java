package ru.yandex.market.logistics.lrm.admin.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.front.library.dto.ExternalReferenceObject;
import ru.yandex.market.logistics.front.library.dto.ReferenceObject;
import ru.yandex.market.logistics.lrm.LrmTest;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminReferenceObjectType;
import ru.yandex.market.logistics.lrm.model.entity.ReturnBoxEntity;
import ru.yandex.market.logistics.lrm.model.entity.ReturnEntity;

@DisplayName("Конвертация ссылок на сущности")
@ParametersAreNonnullByDefault
class AdminReferenceObjectConverterImplTest extends LrmTest {

    private final AdminReferenceObjectConverter referenceObjectConverter = new AdminReferenceObjectConverterImpl();

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Ссылка на возврат")
    void returnReferenceObject(
        @SuppressWarnings("unused") String displayName,
        @Nullable ReturnEntity returnEntity,
        ReferenceObject expectedReference
    ) {
        softly.assertThat(referenceObjectConverter.toReturnReferenceObject(returnEntity))
            .usingRecursiveComparison()
            .isEqualTo(expectedReference);
    }

    @Nonnull
    private static Stream<Arguments> returnReferenceObject() {
        return Stream.of(
            Arguments.of(
                "Успешная конвертация возврата",
                new ReturnEntity().setId(1234L),
                new ReferenceObject(
                    "1234",
                    "1234",
                    "lrm/returns"
                )
            ),
            Arguments.of(
                "Конвертация null",
                null,
                new ReferenceObject()
            )

        );
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Ссылка на грузоместо")
    void returnBoxReferenceObject(
        @SuppressWarnings("unused") String displayName,
        @Nullable ReturnBoxEntity returnBoxEntity,
        ReferenceObject expectedReference
    ) {
        softly.assertThat(referenceObjectConverter.toReturnBoxReferenceObject(returnBoxEntity))
            .usingRecursiveComparison()
            .isEqualTo(expectedReference);
    }

    @Nonnull
    private static Stream<Arguments> returnBoxReferenceObject() {
        return Stream.of(
            Arguments.of(
                "Успешная конвертация грузоместа, все поля заполнены",
                new ReturnBoxEntity().setId(12345L).setExternalId("ext-1"),
                new ReferenceObject(
                    "12345",
                    "ext-1",
                    "lrm/returns/boxes"
                )
            ),
            Arguments.of(
                "Конвертация null",
                null,
                new ReferenceObject()
            ),
            Arguments.of(
                "ExternalId - пустая строка",
                new ReturnBoxEntity().setId(12345L).setExternalId("     "),
                new ReferenceObject(
                    "12345",
                    "12345",
                    "lrm/returns/boxes"
                )
            ),
            Arguments.of(
                "Не указан externalId",
                new ReturnBoxEntity().setId(12345L),
                new ReferenceObject(
                    "12345",
                    "12345",
                    "lrm/returns/boxes"
                )
            )
        );
    }

    @MethodSource
    @DisplayName("Конвертация ссылок на заказ в ЛОМе по его внешнему идентификатору")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void orderExternalReference(
        @SuppressWarnings("unused") String displayName,
        String orderExternalId,
        boolean detailCard,
        ExternalReferenceObject expectedReference
    ) {
        softly.assertThat(referenceObjectConverter.toOrderBarcodeReferenceObject(orderExternalId, detailCard))
            .usingRecursiveComparison()
            .isEqualTo(expectedReference);
    }

    @Nonnull
    private static Stream<Arguments> orderExternalReference() {
        return Stream.of(
            Arguments.of(
                "Для детальной карточки возврата",
                "12345",
                true,
                new ExternalReferenceObject(
                    "Перейти к заказу в LOM",
                    "/lom/orders?barcode=12345",
                    false
                )
            ),
            Arguments.of(
                "Для грида возвратов",
                "12345",
                false,
                new ExternalReferenceObject(
                    "12345",
                    "/lom/orders?barcode=12345",
                    false
                )
            )
        );
    }

    @MethodSource
    @DisplayName("Конвертация ссылок с заданным названием")
    @ParameterizedTest(name = "{1} {0}")
    void namedReferenceObject(
        @SuppressWarnings("unused") String displayName,
        AdminReferenceObjectType referenceObjectType,
        @Nullable Long entityId,
        @Nullable String entityName,
        ReferenceObject expectedReference
    ) {
        softly.assertThat(referenceObjectConverter.toNamedReferenceObject(
            referenceObjectType,
            entityId,
            entityName
        ))
            .usingRecursiveComparison().isEqualTo(expectedReference);
    }

    @Nonnull
    private static Stream<Arguments> namedReferenceObject() {
        List<Arguments> namedReferenceObjectArgs = new ArrayList<>();
        for (AdminReferenceObjectType referenceObjectType : AdminReferenceObjectType.values()) {
            namedReferenceObjectArgs.add(Arguments.of(
                "Переданы null в качестве ид и названия",
                referenceObjectType,
                null,
                null,
                new ReferenceObject()
            ));
            namedReferenceObjectArgs.add(Arguments.of(
                "Передан только идентификатор",
                referenceObjectType,
                123L,
                null,
                new ReferenceObject("123", "123", referenceObjectType.getSlug())
            ));
            namedReferenceObjectArgs.add(Arguments.of(
                "Передано пустое название",
                referenceObjectType,
                123L,
                "       ",
                new ReferenceObject("123", "123", referenceObjectType.getSlug())
            ));
            namedReferenceObjectArgs.add(Arguments.of(
                "Переданы и идентификатор, и название",
                referenceObjectType,
                123L,
                "Лавка 12345",
                new ReferenceObject("123", "Лавка 12345", referenceObjectType.getSlug())
            ));
        }
        return namedReferenceObjectArgs.stream();
    }

    @MethodSource
    @DisplayName("Конвертация ссылок с заданным идентификатором")
    @ParameterizedTest(name = "{1} {0}")
    void referenceObject(
        @SuppressWarnings("unused") String displayName,
        AdminReferenceObjectType referenceObjectType,
        @Nullable Long entityId,
        ReferenceObject expectedReference
    ) {
        softly.assertThat(referenceObjectConverter.toReferenceObject(referenceObjectType, entityId))
            .usingRecursiveComparison().isEqualTo(expectedReference);
    }

    @Nonnull
    private static Stream<Arguments> referenceObject() {
        List<Arguments> referenceObjectArgs = new ArrayList<>();
        for (AdminReferenceObjectType referenceObjectType : AdminReferenceObjectType.values()) {
            referenceObjectArgs.add(Arguments.of(
                "Передан null в качестве ид",
                referenceObjectType,
                null,
                new ReferenceObject()
            ));
            referenceObjectArgs.add(Arguments.of(
                "Передан идентификатор",
                referenceObjectType,
                123L,
                new ReferenceObject("123", "123", referenceObjectType.getSlug())
            ));
        }
        return referenceObjectArgs.stream();
    }
}
