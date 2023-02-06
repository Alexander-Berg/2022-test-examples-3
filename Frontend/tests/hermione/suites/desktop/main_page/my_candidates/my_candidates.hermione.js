const { disableTargetBlank } = require('../../../../helpers');

const PO = {
    notification: '.m-notification',
    myCandidates: '.f-my-candidates',
    candidatesFilterForm: '.f-candidates-filter__form',
    candidatesTableRow: '.f-candidates-table-row:nth-child(1)',
    secondCandidateTableRow: '.f-candidates-table-row:nth-child(2)',
    candidateName: '.f-candidates-table-row:nth-child(1) .f-candidate-field__name',
    candidateVacancy: '.f-candidates-table-row:nth-child(1) .f-candidate-field__vacancy-name',
    radioButtonResponsibleRoleAll: '.f-candidates-filter__row_type_responsible-role .radio-button__radio_side_left',
    radioButtonResponsibleRoleAllChecked: '.f-candidates-filter__row_type_responsible-role .radio-button__radio_side_left.radio-button__radio_checked_yes',
    radioButtonResponsibleRoleRecruiter: '.f-candidates-filter__row_type_responsible-role .radio-button__radio_side_right',
    radioButtonResponsibleRoleRecruiterChecked: '.f-candidates-filter__row_type_responsible-role .radio-button__radio_side_right.radio-button__radio_checked_yes',
    radioButtonResponsibleRoleMainRecruiterChecked: '.f-candidates-filter__row_type_responsible-role .radio-button__radio:nth-child(2).radio-button__radio_checked_yes',
    radioButtonStageAssessmentAllChecked: '.f-candidates-filter__row_type_stage .radio-button__radio:nth-child(1).radio-button__radio_checked_yes',
    radioButtonStageAssessmentAssigned: '.f-candidates-filter__row_type_stage .radio-button__radio:nth-child(2)',
    radioButtonStageAssessmentAssignedChecked: '.f-candidates-filter__row_type_stage .radio-button__radio:nth-child(2).radio-button__radio_checked_yes',
    radioButtonStageAssessmentFinished: '.f-candidates-filter__row_type_stage .radio-button__radio:nth-child(3)',
    radioButtonStageAssessmentFinishedChecked: '.f-candidates-filter__row_type_stage .radio-button__radio:nth-child(3).radio-button__radio_checked_yes',
    radioButtonStageNoAssessment: '.f-candidates-filter__row_type_stage .radio-button__radio:nth-child(4)',
    radioButtonStageNoAssessmentChecked: '.f-candidates-filter__row_type_stage .radio-button__radio:nth-child(4).radio-button__radio_checked_yes',
    candidateTablePagerPageTwo: '.f-candidates-table__pager .staff-pager__page_number_2',
    candidateTablePagerPageTwoActive: '.f-candidates-table__pager .staff-pager__page_number_2.staff-pager__page_current_yes',
    candidateTablePagerPageOneActive: '.f-candidates-table__pager .staff-pager__page_number_1.staff-pager__page_current_yes',
};

const regCandidate = /\/candidates\/[0-9]+[\/]?$/;
const regVacancy = /\/vacancies\/[0-9]+[\/]?$/;

function prepare(browser) {
    return browser
        .conditionalLogin('marat')
        .preparePageExtended('/', [2020, 10, 28, 0, 0, 0])
        .waitForPageLoad()
        .disableAnimations('*')
        .disableFiScrollTo()
        // дожидаемся появления таблиц
        .waitForVisible(PO.myCandidates)
        .waitForVisible(PO.candidatesFilterForm)
        .waitForVisible(PO.candidatesTableRow)
        .waitForLoad()
        //Из-за использования дампов может возникнуть уведомление об отсутствии соединения
        //При клике на уведомление, оно пропадает
        .execute((selector)=>{
            const notification = document.querySelector(selector);
            if( notification ){ notification.click() }
        }, PO.notification);
}

describe('Главная страница / Мои кандидаты', function() {
    it('Список кандидатов', function() {
        return prepare(this.browser)
            .assertView('default', PO.myCandidates);
    });

    it('Ссылка в рассматриваемых вакансиях кандидата', function() {
        return prepare(this.browser)
            .waitForVisible(PO.candidateVacancy)
            .execute(disableTargetBlank, PO.candidateVacancy)
            .click(PO.candidateVacancy)
            .assertUrlReg(regVacancy);
    });

    it('Ссылка в имени кандидата', function() {
        return prepare(this.browser)
            .execute(disableTargetBlank, PO.candidateName)
            .click(PO.candidateName)
            .assertUrlReg(regCandidate);
    });

    it('Фильтр по роли', function() {
        return prepare(this.browser)
            .click(PO.radioButtonResponsibleRoleAll)
            .waitForLoad()
            .waitForVisible(PO.secondCandidateTableRow)
            .assertView('responsible_role_all', PO.myCandidates)
            .click(PO.radioButtonResponsibleRoleRecruiter)
            .waitForLoad()
            .waitForVisible(PO.secondCandidateTableRow)
            .assertView('responsible_role_recruiter', PO.myCandidates);
    });

    it('Фильтр по испытаниям', function() {
        return prepare(this.browser)
            .waitForEnabled(PO.radioButtonStageAssessmentAssigned)
            .click(PO.radioButtonStageAssessmentAssigned)
            /*
            Таблицы реализованы таким образом, что при загрузке данных кандидаты могут появится до того как пропадет спинер,
            и спинер может пропасть, но будет отображаться только первый кандидат из всех (те таблица не загрузилась до конца)
            Варианта лучше нет - дождаться пока пропадет спиннер загрузки и дождаться появления второго кандидата в списке, либо ставить паузу
            */
            .waitForLoad()
            .waitForVisible(PO.secondCandidateTableRow)
            .assertView('stage_assessment_assigned', PO.myCandidates)
            .waitForEnabled(PO.radioButtonStageAssessmentFinished)
            .click(PO.radioButtonStageAssessmentFinished)
            .waitForLoad()
            .waitForVisible(PO.secondCandidateTableRow)
            .assertView('stage_assessment_finished', PO.myCandidates)
            .waitForEnabled(PO.radioButtonStageNoAssessment)
            .click(PO.radioButtonStageNoAssessment)
            .waitForLoad()
            .waitForVisible(PO.secondCandidateTableRow)
            .assertView('stage_no_assessment', PO.myCandidates);
    });

    it('Пагинация в блоке кандидатов', function() {
        return prepare(this.browser)
            .click(PO.candidateTablePagerPageTwo)
            .waitForHidden(PO.candidateTablePagerPageOneActive)
            .waitForVisible(PO.candidateTablePagerPageTwoActive, 10000)
            .waitForVisible(PO.candidatesTableRow, 10000)
            .assertView('pagination', PO.myCandidates);
    });
});
