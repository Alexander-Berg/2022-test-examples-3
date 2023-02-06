const PO = require('../../../../page-objects/pages/vacancy');

describe('Вакансии / Смена статуса вакансии', function() {
    it('Перевод статуса вакансии "В работу"', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/vacancies/54219/')
            .disableAnimations('.popup2')
            .waitForVisible(PO.fPageVacancy.actions())
            .assertView('status_on_approval', PO.fPageVacancy.header())
            .click(PO.fPageVacancy.actions.menuSwitcher())
            .waitForVisible(PO.vacancyApprove())
            .assertView('actions', `${PO.popup2Visible()} .menu`)
            .click(PO.vacancyApprove())
            .waitForVisible(PO.inProgressForm.main_recruiter())
            .pause(200)
            .setValue(PO.inProgressForm.bpId.input(), '9911')
            .assertView('workflow_form', PO.inProgressForm())
            .setSuggestValue({
                block: PO.inProgressForm.main_recruiter.input(),
                menu: PO.mainRecruitersSuggest.items(),
                text: 'Марат Январев',
                item: PO.mainRecruitersSuggest.marat(),
            })
            .click(PO.inProgressForm.submit())
            .waitForHidden(PO.inProgressForm())
            .assertText(PO.vacancyInfo.bpId(), '9911')
            .assertText(PO.vacancyInfo.mainRecruiter.username(), 'Марат Январев')
            .assertView('status_in_progress', PO.fPageVacancy.header());
    });
});
