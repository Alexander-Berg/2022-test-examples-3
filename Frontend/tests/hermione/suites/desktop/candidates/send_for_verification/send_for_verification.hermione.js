const PO = require('../../../../page-objects/pages/candidate');

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

describe('Кандидат / Конфликт интересов', function() {
    it('Проверка конфликта интересов', function() {
        return this.browser
            .then(() => prepareBrowser(this.browser))
            .waitForVisible(PO.pageCandidate.actions())
            .assertView('candidate_actions', PO.pageCandidate.actions())
            .click(PO.pageCandidate.actions.menu())
            .waitForVisible(PO.actionCreateVerification())
            .assertView('candidate_actions_with_action_create_verification', PO.menu())
            .click(PO.actionCreateVerification())
            .waitForVisible(PO.candidateActionCreateVerificationDialog.form())
            .staticElement(PO.candidateActionCreateVerificationDialog())
            .waitForHidden(PO.candidateActionCreateVerificationDialog.progress())
            .assertView('candidate_action_verification_form', PO.candidateActionCreateVerificationDialog.form())
            .setSelectValue({
                block: PO.candidateActionCreateVerificationDialog.form.fieldTypeApplication(),
                menu: PO.applicationsPopup(),
                item: PO.applicationsPopup.firstApplication(),
            })
            .click(PO.candidateActionCreateVerificationDialog.form.submit())
            .waitForHidden(PO.candidateActionCreateVerificationDialog.form())
            .click(PO.pageCandidate.actions.menu())
            .assertView('candidate_workflow_actions', PO.menu())
            .assertView('candidate_conflict_of_interests_info', PO.pageCandidate.conflictOfInterestsInfo());
    });
});
