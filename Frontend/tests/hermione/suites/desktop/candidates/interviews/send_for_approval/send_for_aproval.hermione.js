const { setAjaxHash } = require('../../../../../helpers');

const PO = require('../../../../../page-objects/pages/candidate');

describe('Кандидат.Испытания / Отправка секции на согласование', function() {
    it('Отправка секции на согласование', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended('/candidates/200000062/applications/', [2019, 10, 24, 0, 0, 0], 'candidates/1')
            .waitForVisible(PO.actualApplications.firstApplication())
            .assertView('actual_applications_before_sending', PO.actualApplications())
            .waitForVisible(PO.applPane.activeConsiderationApplications())
            .assertView('all_applications_before_sending', PO.applPane.activeConsiderationApplications())
            .click(PO.interviewsTab())
            .waitForVisible(PO.sendForApproval())
            .assertView('send_for_approval_button', PO.sendForApproval())
            .click(PO.sendForApproval())
            .waitForVisible(PO.sendForApprovalForm())
            .setSelectValue({
                block: PO.sendForApprovalForm.application(),
                menu: PO.sendForApprovalTypeSelect.menu(),
                item: PO.sendForApprovalTypeSelect.vac51790(),
            })
            .assertView('send_for_approval_form_filled', PO.sendForApprovalForm())
            .execute(setAjaxHash, 'after_send_for_approval')
            .click(PO.sendForApprovalForm.submit())
            .waitForHidden(PO.sendForApprovalForm())
            .click(PO.applicationsTab())
            .waitForHidden(PO.actualApplications.secondApplication())
            .assertView('actual_applications_after_sending', PO.actualApplications())
            .waitUntil(() => {
                return this.browser
                    .getText(PO.applPane.activeConsiderationApplications.firstApplication.status.text())
                    .then(text => text === 'Закрыт');
            }, 0, 'Вакансия не перешла в статус "Закрыт"', 100)
            .assertView('all_applications_after_sending', PO.applPane.activeConsiderationApplications());
    });

    it('Проверка обязательных полей', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/candidates/200000062/interviews/')
            .waitForVisible(PO.sendForApproval())
            .click(PO.sendForApproval())
            .waitForVisible(PO.sendForApprovalForm())
            .click(PO.sendForApprovalForm.submit())
            .assertView('send_for_approval_form_empty', PO.sendForApprovalForm());
    });
});
