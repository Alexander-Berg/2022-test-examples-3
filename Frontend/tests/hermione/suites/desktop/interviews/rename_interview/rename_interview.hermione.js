const PO = require('../../../../page-objects/pages/interview');

describe('Секция / Переименование секции', function() {
    it('Переименование секции', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/interviews/33122/')
            .waitForVisible(PO.interviewPage())
            .assertView('rename_button', PO.interviewPage.actions.rename())
            .click(PO.interviewPage.actions.rename())
            .waitForVisible(PO.renameForm.field())
            .setValue(PO.renameForm.field(), 'Новое название')
            .click(PO.renameForm.submit())
            .waitForHidden(PO.renameForm());
    });
});
