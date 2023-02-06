const PO = require('../../../../page-objects/pages/candidate');
const RPO = require('../../../../page-objects/react-pages/candidate');

/**
 * Общая подготовка перед каждым тест-кейсом
 * @param {Object} browser
 * @returns {Object}
 */
function prepareBrowser(browser) {
    return browser
        .conditionalLogin('marat')
        .preparePageExtended('/candidates/98217948/', [2019, 10, 24, 0, 0, 0], 'candidates/1')
        .disableAnimations('*');
}

describe('Кандидат / Отправка сертификата', function() {
    it('Проверка отправки сертификата', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidate.actions())
            .assertView('candidate_actions', PO.pageCandidate.actions())
            .click(PO.pageCandidate.actions.menu())
            .waitForVisible(PO.actionCertificationCreate())
            .assertView('candidate_actions_with_action_certification_create', PO.menu())
            .click(PO.actionCertificationCreate())
            .waitForVisible(PO.candidateActionCertificationCreateDialog.form())
            .staticElement(PO.candidateActionCertificationCreateDialog())
            .waitForHidden(PO.candidateActionCertificationCreateDialog.progress())
            .assertView('certification_create', PO.candidateActionCertificationCreateDialog.form())
            .setReactSFieldValue(RPO.candidateCreateCertificateForm.consideration(), 2, 'select')
            .assertView('certification_create_filled', PO.candidateActionCertificationCreateDialog.form())
            .click(RPO.candidateCreateCertificateForm.submit())
            .waitForHidden(PO.candidateActionCertificationCreateDialog.form());
    });
});
