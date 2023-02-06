package ru.yandex.market.adv.b2bmonetization.programs.interactor.sorryManual;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@DisplayName("Тесты на endpoint POST /v1/program")
class PostSorryManualProgramApiServiceTest extends AbstractMonetizationTest {

    @DisplayName("Успешно зарегистрировали повторно программу SORRY с указанными суммой бонуса и сроком действия")
    @DbUnitDataSet(
            before = "Post/csv/postProgramSorry_withSumAndExpired_success.before.csv",
            after = "Post/csv/postProgramSorry_withSumAndExpired_success.after.csv"
    )
    @Test
    void postProgramSorry_withSumAndExpired_success() {
        postOk("postProgramSorry_withSumAndExpired_success.json", "1");
    }

    @DisplayName("400. Не указан срок действия бонуса в программе SORRY")
    @DbUnitDataSet(
            before = "Post/csv/postProgramSorry_withSumWithoutExpired_exception.before.csv"
    )
    @Test
    void postProgramSorry_withSumWithoutExpired_exception() {
        postWrong("postProgramSorry_withSumWithoutExpired_exception.json", "1");
    }

    @DisplayName("400. Не указана сумма бонуса в программе SORRY")
    @DbUnitDataSet(
            before = "Post/csv/postProgramSorry_withoutSum_exception.before.csv"
    )
    @Test
    void postProgramSorry_withoutSum_exception() {
        postWrong("postProgramSorry_withoutSum_exception.json", "1");
    }

    @DisplayName("Успешно зарегистрировали программу SORRY впервые для партнера")
    @DbUnitDataSet(
            before = "Post/csv/postProgramSorry_firstBonus_success.before.csv",
            after = "Post/csv/postProgramSorry_firstBonus_success.after.csv"
    )
    @Test
    void postProgramSorry_firstBonus_success() {
        postOk("postProgramSorry_firstBonus_success.json", "1");
    }

    @DisplayName("400. Слишком частые запросы на регистрацию программы SORRY.")
    @DbUnitDataSet(
            before = "Post/csv/postProgramSorry_secondBonusIn10Min_badRequest.before.csv"
    )
    @Test
    void postProgramSorry_secondBonusIn10Min_badRequest() {
        postWrong("postProgramSorry_secondBonusIn10Min_badRequest.json", "1");
    }

    @DisplayName("400. Партнер не зарегисрирован и не является участником программы SORRY")
    @DbUnitDataSet(
            before = "Post/csv/postProgramSorry_notParticipant_badRequest.before.csv"
    )
    @Test
    void postProgramSorry_notParticipant_badRequest() {
        postWrong("postProgramSorry_notParticipant_badRequest.json", "1");
    }

    @DisplayName("Успешно зарегистрировали повторно программу MANUAL с указанными суммой бонуса и сроком действия")
    @DbUnitDataSet(
            before = "Post/csv/postProgramManual_withSumAndExpired_success.before.csv",
            after = "Post/csv/postProgramManual_withSumAndExpired_success.after.csv"
    )
    @Test
    void postProgramManual_withSumAndExpired_success() {
        postOk("postProgramManual_withSumAndExpired_success.json", "1");
    }

    @DisplayName("400. Не указан срок действия бонуса в программе MANUAL")
    @DbUnitDataSet(
            before = "Post/csv/postProgramManual_withSumWithoutExpired_exception.before.csv"
    )
    @Test
    void postProgramManual_withSumWithoutExpired_exception() {
        postWrong("postProgramManual_withSumWithoutExpired_exception.json", "1");
    }

    @DisplayName("400. Не указана сумма бонуса в программе MANUAL")
    @DbUnitDataSet(
            before = "Post/csv/postProgramManual_withoutSum_exception.before.csv"
    )
    @Test
    void postProgramManual_withoutSum_exception() {
        postWrong("postProgramManual_withoutSum_exception.json", "1");
    }

    @DisplayName("Успешно зарегистрировали программу MANUAL впервые для партнера")
    @DbUnitDataSet(
            before = "Post/csv/postProgramManual_firstBonus_success.before.csv",
            after = "Post/csv/postProgramManual_firstBonus_success.after.csv"
    )
    @Test
    void postProgramManual_firstBonus_success() {
        postOk("postProgramManual_firstBonus_success.json", "1");
    }

    @DisplayName("400. Слишком частые запросы на регистрацию программы MANUAL")
    @DbUnitDataSet(
            before = "Post/csv/postProgramManual_secondBonusIn10Min_badRequest.before.csv"
    )
    @Test
    void postProgramManual_secondBonusIn10Min_badRequest() {
        postWrong("postProgramManual_secondBonusIn10Min_badRequest.json", "1");
    }

    @DisplayName("400. Партнер не зарегисрирован и не является участником программы MANUAL")
    @DbUnitDataSet(
            before = "Post/csv/postProgramManual_notParticipant_badRequest.before.csv"
    )
    @Test
    void postProgramManual_notParticipant_badRequest() {
        postWrong("postProgramManual_notParticipant_badRequest.json", "1");
    }

    @DisplayName("400. Дата окончания действия бонуса раньше, чем текущая дата, для программы SORRY.")
    @DbUnitDataSet(
            before = "Post/csv/postProgramSorry_wrongExpiredAt_badRequest.before.csv"
    )
    @Test
    void postProgramSorry_wrongExpiredAt_badRequest() {
        postWrong("postProgramSorry_wrongExpiredAt_badRequest.json", "1");
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
}
