const PO = require('../../../../page-objects/pages/application');

describe('Претендент / Закрытие', function() {
    it('Проверка обязательных полей', function() {
        const APP_ID = '1003974'; // должно быть в работе

        return this.browser
            .conditionalLogin('marat')
            .preparePage('', `/applications/${APP_ID}/`)
            .disableAnimations('*')
            .waitForVisible(PO.pageApplication.actions())
            .assertView('actions_menu', PO.pageApplication.actions())
            .click(PO.pageApplication.actions.menu())
            .waitForVisible(PO.applicationMenuPopup())
            .assertView('actions', PO.applicationMenuPopup())
            .click(PO.applicationActionClose())
            .waitForVisible(PO.applicationActionCloseDialog.form())
            .waitForHidden(PO.applicationActionCloseDialog.progress())
            .assertView('close_form', PO.applicationActionCloseDialog.form())
            .click(PO.applicationActionCloseDialog.form.submit())
            .assertView('close_form_errors', PO.applicationActionCloseDialog.form());
    });
    it('Закрытие претендента', function() {
        const APP_ID = '1003974'; // должна быть в работе

        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended(`/applications/${APP_ID}/`, [2019, 12, 22, 0, 0, 0], '/applications/')
            .disableAnimations('*')
            .waitForVisible(PO.pageApplication.actions())
            .assertView('actions_menu', PO.pageApplication.actions())
            .click(PO.pageApplication.actions.menu())
            .waitForVisible(PO.applicationMenuPopup())
            .assertView('actions', PO.applicationMenuPopup())
            .click(PO.applicationActionClose())
            .waitForVisible(PO.applicationActionCloseDialog.form())
            .waitForHidden(PO.applicationActionCloseDialog.progress())
            .assertView('close_form', PO.applicationActionCloseDialog.form())
            .setSelectValue({
                block: PO.applicationActionCloseDialog.fieldTypeResolution(),
                menu: PO.resolutionsSelect(),
                item: PO.resolutionsSelect.didNotPassAssessments(),
            })
            .assertView('close_form_filled', PO.applicationActionCloseDialog.form())
            .click(PO.applicationActionCloseDialog.form.submit())
            .waitForHidden(PO.applicationActionCloseDialog.form())
            .waitForVisible(PO.pageApplication.statusClosed())
            .assertView('closed_application_header', PO.pageApplication.header())
            .click(PO.pageApplication.actions.menu())
            .waitForVisible(PO.applicationMenuPopup())
            .assertView('closed_application_actions', PO.applicationMenuPopup());
    });
});
