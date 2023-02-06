package ru.yandex.market.tpl.core.query.usershift.mapper.expendable;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.task.expendable.CountDto;
import ru.yandex.market.tpl.api.model.task.expendable.CountUnit;
import ru.yandex.market.tpl.api.model.task.expendable.ExpendableDto;
import ru.yandex.market.tpl.api.model.task.expendable.ExpendableType;
import ru.yandex.market.tpl.api.model.task.expendable.ExpendablesDto;

import static org.assertj.core.api.Assertions.assertThat;

class ExpendablesMapperTest {

    @Test
    void map() {
        var mapper = new ExpendablesMapper();
        var result = mapper.map(List.of(
                new Expendable(ExpendableType.PAYMENT_TERMINAL, 1),
                new Expendable(ExpendableType.RETURN_PACKING_TAPE, 1),
                new Expendable(ExpendableType.RETURN_BARCODE_SHEET, 2)
        ));

        assertThat(result).isEqualTo(new ExpendablesDto(List.of(
            new ExpendableDto("Терминал для оплаты", new CountDto(1, CountUnit.PIECE)),
            new ExpendableDto("Скотч для упаковки возвратов", new CountDto(1, CountUnit.PIECE)),
            new ExpendableDto("Штрихкоды для возвратов", new CountDto(2, CountUnit.PACK))
        )));
    }

}
