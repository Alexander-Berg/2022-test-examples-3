package ru.yandex.market.tpl.core.domain.usershift.partner;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kukabara
 */
class PartnerRoutingListDataDtoMapperTest {

    private PartnerRoutingListDataDtoMapper mapper = new PartnerRoutingListDataDtoMapper(null,
            null, null, null);

    @Test
    void checkNull() {
        assertThat(mapper.addSpaces(null, 3)).isNull();
    }

    @Test
    void withoutSpaces() {
        assertThat(mapper.addSpaces("ABCDEFG", 3)).isEqualTo("ABC DEF G");
    }

    @Test
    void lessLength() {
        assertThat(mapper.addSpaces("ABCDEFG", 7)).isEqualTo("ABCDEFG");
    }

    @Test
    void withSpaces() {
        assertThat(mapper.addSpaces(" ABC\tDEF\n\tG", 3)).isEqualTo(" ABC\tDEF\n\tG");
    }

    @Test
    void checkRealNote() {
        assertThat(mapper.addSpaces("ПОЗВОНИТЬ ЗА ЧАС!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" +
                        "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" +
                        "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" +
                        "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!",
                25)).isEqualTo("ПОЗВОНИТЬ ЗА ЧАС!!!!!!!!!!!!!!!!!!!!!! !!!!!!!!!!!!!!!!!!!!!!!!! " +
                "!!!!!!!!!!!!!!!!!!!!!!!!! !!!!!!!!!!!!!!!!!!!!!!!!! !!!!!!!!!!!!!!!!!!!!!!!!! " +
                "!!!!!!!!!!!!!!!!!!!!!!!!! !!!!!!!!!!!!!!!!!!!!!!!!! !!!!!!!!!!!!!!!!!!!!!!!!! " +
                "!!!!!!!!!!!!!!!!!!!!!!!!! !!!!!!!!!!!!!!!!!!!!!!!!! !!!!!!!!!!!!!!!!!!!!!!!!! !!!!!!!!!!!!!!!!");
    }

}
