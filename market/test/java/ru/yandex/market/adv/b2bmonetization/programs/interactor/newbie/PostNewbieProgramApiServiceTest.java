package ru.yandex.market.adv.b2bmonetization.programs.interactor.newbie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@DisplayName("Тесты на endpoint POST /v1/program")
class PostNewbieProgramApiServiceTest extends AbstractMonetizationTest {

    @DbUnitDataSet(
            before = "Post/csv/v1ProgramPost_sendFalse_newRecordNotEnqueued.before.csv",
            after = "Post/csv/v1ProgramPost_sendFalse_newRecordNotEnqueued.after.csv"
    )
    @DisplayName("Партнер не участвовал в программе. Партнер отклонил участие в программе - REFUSED")
    @Test
    void v1ProgramPost_sendFalse_newRecordNotEnqueued() {
        run("v1ProgramPost_sendFalse_newRecordNotEnqueued_",
                () -> postOk("v1ProgramPost_sendFalse_newRecordNotEnqueued.json", "421")
        );
    }

    @DbUnitDataSet(
            before = "Post/csv/v1ProgramPost_sendTrue_updateResetToNew.before.csv",
            after = "Post/csv/v1ProgramPost_sendTrue_updateResetToNew.after.csv"
    )
    @DisplayName("Обновили статус из RESET в NEW и повторно выполнили запрос.")
    @Test
    void v1ProgramPost_sendTrue_updateResetToNew() {

        run("v1ProgramPost_sendTrue_updateResetToNew_",
                () -> postOk("v1ProgramPost_sendTrue_updateResetToNew.json", "523")
        );
    }

    @DbUnitDataSet(
            before = "Post/csv/v1ProgramPost_sendFalseAndThenTrue_newRecordWithEnqueuedTrue.before.csv",
            after = "Post/csv/v1ProgramPost_sendFalseAndThenTrue_newRecordWithEnqueuedTrue.after.csv"
    )
    @DisplayName("Создали новую программу в статусе NEW с enabled = false")
    @Test
    void v1ProgramPost_sendFalseAndThenTrue_newRecordWithEnqueuedTrue() {
        run("v1ProgramPost_sendFalseAndThenTrue_newRecordWithEnqueuedTrue_",
                () -> postOk("v1ProgramPost_sendFalseAndThenTrue_newRecordWithEnqueuedTrue.json", "523")
        );
    }

    @DisplayName("400. Неверный идентификатор пользователя")
    @Test
    void v1ProgramPost_wrongParam_incorrectUid() {
        postWrong("v1ProgramPost_wrongParam_incorrectUid.json", "hgasd");
    }

    @DisplayName("400. Неверный статус программы")
    @Test
    void v1ProgramPost_wrongParam_incorrectStatus() {
        postWrong("v1ProgramPost_wrongParam_incorrectStatus.json", "523");
    }

    @DisplayName("400. Неверный тип программы")
    @Test
    void v1ProgramPost_wrongParam_incorrectProgramType() {
        postWrong("v1ProgramPost_wrongParam_incorrectProgramType.json", "523");
    }

    @DisplayName("400. Статус программы не поддерживается")
    @Test
    void v1ProgramPost_wrongParam_notSupportStatus() {
        run("v1ProgramPost_wrongParam_notSupportStatus_",
                () -> postWrong("v1ProgramPost_wrongParam_notSupportStatus.json", "523")
        );
    }

    @DbUnitDataSet(
            before = "Post/csv/v1ProgramPost_checkPartnerInPg_newRecordNotEnqueued.before.csv",
            after = "Post/csv/v1ProgramPost_checkPartnerInPg_newRecordNotEnqueued.after.csv"
    )
    @DisplayName("Создали новую программу с проверкой партнера в PG")
    @Test
    void v1ProgramPost_checkPartnerInPg_newRecordNotEnqueued() {
        run("v1ProgramPost_checkPartnerInPg_newRecordNotEnqueued_",
                () -> postOk("v1ProgramPost_checkPartnerInPg_newRecordNotEnqueued.json", "523")
        );
    }

    private void postWrong(String fileName, String uid) {
        mvcPerform(
                HttpMethod.POST,
                "/v1/program?uid=" + uid,
                HttpStatus.BAD_REQUEST.value(),
                "Post/json/response/" + fileName,
                "Post/json/request/" + fileName,
                true
        );
    }

    private void postOk(String fileName, String uid) {
        mvcPerform(
                HttpMethod.POST,
                "/v1/program?uid=" + uid,
                HttpStatus.NO_CONTENT.value(),
                null,
                "Post/json/request/" + fileName,
                false
        );
    }
}
