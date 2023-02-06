const PO = require('../../../../page-objects/pages/candidate');
const RPO = require('../../../../page-objects/react-pages/candidate');

describe('Кандидат / Закрытие', function() {
    it('Закрытие кандидата. Единственный рекрутер', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/candidates/77594805')
            .waitForVisible(PO.pageCandidate.actions())
            .disableAnimations('*')
            .waitForLoad()
            .click(PO.pageCandidate.actions.menu())
            .waitForVisible(PO.menuPopup())
            .assertView('actions_menu', PO.menuPopup())
            .click(PO.actionCloseCandidate())
            .waitForVisible(RPO.candidateCloseForm())
            .assertView('close_form', RPO.candidateCloseForm())
            .click(RPO.candidateCloseForm.resolutionSelect.button())
            .waitForVisible(RPO.candidateCloseForm.resolutionSelect.popup())
            .click(RPO.candidateCloseForm.resolutionSelect.rejectedAfterTestTask())
            .waitForHidden(RPO.candidateCloseForm.resolutionSelect.popup())
            .click(RPO.candidateCloseForm.submit())
            .waitForHidden(RPO.candidateCloseForm())
            .waitForVisible(PO.pageCandidate.statusClosed())
            .assertView('closed_candidate_header', PO.pageCandidate.header())
            .click(PO.pageCandidate.actions.menu())
            .waitForVisible(PO.menuPopup())
            .assertView('closed_candidate_actions', PO.menuPopup());
    });
});
