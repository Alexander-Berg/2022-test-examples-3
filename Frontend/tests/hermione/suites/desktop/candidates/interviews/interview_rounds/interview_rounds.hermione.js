const RPO = require('../../../../../page-objects/react-pages/candidate');
const { setAjaxHash } = require('../../../../../helpers');

describe('Кандидат.Испытания / Черновики серий секций', function() {
    it('Отмена удаления черновика серии секций', function() {
        return this.browser
            .conditionalLogin('marat')
            // открыть кандидата с черновиками серий секций /candidates/<id_кандидата>/interviews/
            .preparePage('', '/candidates/200016924/interviews/')
            .waitForPageLoad()
            .disableAnimations('*')
            // ждем загрузки данных об испытаниях кандидата
            .waitForVisible(RPO.interviewRound())
            // скриншот: список черновиков серий секций
            .assertView('interviews_rounds_list', RPO.interviewRoundsList())
            // клик на иконку корзины
            .click(RPO.interviewRound.interviewRoundActions.deleteButton())
            // ждем появления модального окна с подтверждением удаления
            .waitForVisible(RPO.interviewRoundsModal())
            // скриншот: внешний вид модального окна
            .assertView('modal', RPO.interviewRoundsModal.content())
            // клик на кнопку 'Отмена'
            .click(RPO.interviewRoundsModalCancelButton())
            // ждем закрытия модального окна
            .waitForHidden(RPO.interviewRoundsModal())
            .waitForVisible(RPO.interviewRoundsList())
            // скриншот: список испытаний не изменился
            .assertView('interviews_rounds_list_not_changed', RPO.interviewRoundsList());
    });

    it('Удаление черновика серии секций', function() {
        let count = 0
        return this.browser
            .conditionalLogin('marat')
            // открыть кандидата с черновиками серий секций /candidates/<id_кандидата>/interviews/
            .preparePage('', '/candidates/200016924/interviews/')
            .waitForPageLoad()
            .disableAnimations('*')
            // ждем загрузки данных об испытаниях кандидата
            .waitForVisible(RPO.interviewRound())
            // скриншот: список черновиков серий секций
            .assertView('interviews_rounds_list', RPO.interviewRoundsList())
            // клик на иконку корзины
            .elements(RPO.interviewRounds())
            .then((arr) => {
                count = arr.length
            })
            .click(RPO.interviewRound.interviewRoundActions.deleteButton())
            // ждем появления модального окна с подтверждением удаления
            .waitForVisible(RPO.interviewRoundsModal())
            .pause(5000)
            // скриншот: внешний вид модального окна.
            .assertView('modal', RPO.interviewRoundsModal.content())
            // добавляем к следующим запросам параметр,
            // чтобы клемент заново закэшировал запрос за списком испытаний кандидата
            .execute(setAjaxHash, 'after_delete')
            // клик на кнопку 'Удалить'
            .click(RPO.interviewRoundsModalSubmitButton())
            // ждем закрытия модального окна
            .waitForHidden(RPO.interviewRoundsModal())
            .waitForVisible(RPO.interviewRoundsList())
            .assertView('clear_interviews_list', RPO.interviewRoundsList());
    });
});
