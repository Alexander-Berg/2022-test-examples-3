package ru.yandex.market.adv.b2bmonetization.programs.interactor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@DisplayName("Тесты на endpoint PUT /v1/program/status")
public class ProgramStatusInteractorTest extends AbstractMonetizationTest {

    @DisplayName("Проверка смены статуса заявок участия в программах.")
    @DbUnitDataSet(
            before = "ProgramStatusInteractor/csv/programStatus_changeStatus_setReady.before.csv",
            after = "ProgramStatusInteractor/csv/programStatus_changeStatus_setReady.after.csv"
    )
    @Test
    public void programStatus_changeStatus_setReady() {
        mvcPerform(
                HttpMethod.PUT,
                "/v1/program/status",
                HttpStatus.NO_CONTENT.value(),
                null,
                "ProgramStatusInteractor/json/request/programStatus_changeStatus_setReady.json",
                false
        );
    }
}
