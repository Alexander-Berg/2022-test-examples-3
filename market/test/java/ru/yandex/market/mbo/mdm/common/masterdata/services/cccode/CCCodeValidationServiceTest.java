package ru.yandex.market.mbo.mdm.common.masterdata.services.cccode;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CustomsCommCodeRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.CCCodeValidationError.Type;
import ru.yandex.market.mboc.common.masterdata.model.cccode.CustomsCommCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("checkstyle:MagicNumber")
public class CCCodeValidationServiceTest {

    private CustomsCommCodeRepositoryMock codeRepository;
    private CCCodeValidationService service;

    @Before
    public void setup() {
        codeRepository = new CustomsCommCodeRepositoryMock();
        service = new CCCodeValidationService(List.of(), codeRepository);
    }

    @Test
    public void whenInvalidCreateShouldThrow() {
        assertThatThrownBy(() -> service.validateCreate(null))
            .isExactlyInstanceOf(NullPointerException.class)
            .hasMessageContaining("Got null as customs commodity code during creation");

        assertThatThrownBy(() -> service.validateCreate(new CustomsCommCode().setId(10)))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Trying to create an object with ID > 0");
    }

    @Test
    public void whenInvalidUpdateShouldThrow() {
        assertThatThrownBy(() -> service.validateUpdate(null))
            .isExactlyInstanceOf(NullPointerException.class)
            .hasMessageContaining("Got null as customs commodity code during update");

        assertThatThrownBy(() -> service.validateUpdate(new CustomsCommCode().setId(0)))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Trying to update non-existing object with ID == 0");

        assertThatThrownBy(() -> service.validateUpdate(new CustomsCommCode().setCode("ololo").setId(10)))
            .isExactlyInstanceOf(NullPointerException.class)
            .hasMessageContaining("Cannot find existing code ololo by id 10");
    }

    @Test
    public void whenInvalidDeleteShouldThrow() {
        assertThatThrownBy(() -> service.validateDelete(0))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Zero id provided for deletion");

        assertThatThrownBy(() -> service.validateDelete(10))
            .isExactlyInstanceOf(NullPointerException.class)
            .hasMessageContaining("Cannot find existing code by id 10");
    }

    @Test
    public void whenInputIsValidShouldContinue() {
        codeRepository.insert(new CustomsCommCode().setCode("ololo").setId(10));
        assertThat(service.validateCreate(new CustomsCommCode().setId(0))).isEmpty();
        assertThat(service.validateUpdate(new CustomsCommCode().setId(10))).isEmpty();
        assertThat(service.validateDelete(10)).isEmpty();
    }

    @Test
    public void testSomeSimpleValidations() {
        service = new CCCodeValidationService(List.of(new CodeFormatCCCodeValidator(),
            new TitleFormatCCCodeValidator()), codeRepository);
        assertThat(service.validateCreate(new CustomsCommCode().setCode("ololo").setTitle("     ").setId(0)))
            .containsExactlyInAnyOrder(errorOf(Type.EMPTY_TITLE), errorOf(Type.CODE_NOT_NUMERIC));
    }

    private CCCodeValidationError errorOf(CCCodeValidationError.Type errorType) {
        return new CCCodeValidationError(errorType, "");
    }
}
