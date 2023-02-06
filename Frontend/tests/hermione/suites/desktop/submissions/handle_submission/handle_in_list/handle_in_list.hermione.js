const PO = require('../../../../../page-objects/pages/submissions');

describe('Отклики / Обработка в списке', function() {
    it('Отправка в спам', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended('/submissions/?source=form&recruiter=', [2019, 8, 22, 0, 0, 0], '/submissions/1')
            .disableAnimations('*')
            .waitForVisible(PO.submissionsTable.first())
            .assertView('submission_in_table', PO.submissionsTable.first())
            .staticElement(PO.sidePopup())
            .click(PO.submissionsTable.first.id())
            .waitForVisible(PO.fSubmission.actionReject())
            .scroll(PO.fSubmission.actionHandle())
            .assertView('reject_button', PO.fSubmission.actionReject())
            .click(PO.fSubmission.actionReject())
            .waitForVisible(PO.fSubmissionWorkflowForm.header())
            .assertView('spam_form', PO.fSubmissionWorkflowForm())
            .click(PO.fSubmissionWorkflowForm.submit())
            .waitForHidden(PO.fSubmissionWorkflowForm())
            .assertView('handled_submission', PO.fSubmission())
            .assertView('handled_submission_in_table', PO.submissionsTable.first());
    });
    it('Создание кандидата и отказ', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended('/submissions/?source=form&recruiter=', [2019, 8, 22, 0, 0, 0], '/submissions/1')
            .disableAnimations('*')
            .waitForVisible(PO.submissionsTable.first())
            .assertView('submission_in_table', PO.submissionsTable.first())
            .staticElement(PO.sidePopup())
            .click(PO.submissionsTable.first.id())
            .waitForVisible(PO.fSubmission.actionHandle())
            .scroll(PO.fSubmission.actionHandle())
            .assertView('handle_button', PO.fSubmission.actionHandle())
            .staticElement(PO.modal())
            .click(PO.fSubmission.actionHandle())
            .waitForVisible(PO.fSubmissionWorkflowForm.header())
            .assertView('handle_form_empty', PO.fSubmissionWorkflowForm())
            .click(PO.fSubmissionWorkflowForm.createCandidate())
            .pause(50) // Радиобатоны мигают
            .click(PO.fSubmissionWorkflowForm.isRejectionField())
            .assertView('handle_form_filled', PO.fSubmissionWorkflowForm())
            .click(PO.fSubmissionWorkflowForm.submit())
            .waitForHidden(PO.fSubmissionWorkflowForm())
            .assertView('handled_submission', PO.fSubmission())
            .assertView('handled_submission_in_table', PO.submissionsTable.first())
            .click(PO.submissionsTable.first.candidateLink())
            .assertUrl('/candidates/200015666');
    });
    it('Мерж к кандидату', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended('/submissions/?source=form&recruiter=', [2019, 8, 22, 0, 0, 0], '/submissions/1')
            .disableAnimations('*')
            .waitForVisible(PO.submissionsTable.first())
            .assertView('submission_in_table', PO.submissionsTable.first())
            .staticElement(PO.sidePopup())
            .click(PO.submissionsTable.first.id())
            .waitForVisible(PO.fSubmission.actionHandle())
            .scroll(PO.fSubmission.actionHandle())
            .assertView('handle_button', PO.fSubmission.actionHandle())
            .click(PO.fSubmission.actionHandle())
            .waitForVisible(PO.fSubmissionWorkflowForm.header())
            .assertView('handle_form_empty', PO.fSubmissionWorkflowForm())
            .click(PO.fSubmissionWorkflowForm.duplicatesField.first.radio())
            .pause(50) // Радиобатоны мигают
            .assertView('handle_form_filled', PO.fSubmissionWorkflowForm())
            .click(PO.fSubmissionWorkflowForm.submit())
            .waitForHidden(PO.fSubmissionWorkflowForm())
            .assertView('handled_submission', PO.fSubmission())
            .assertView('handled_submission_in_table', PO.submissionsTable.first())
            .click(PO.submissionsTable.first.candidateLink())
            .assertUrl('/candidates/200001129');
    });
    it('Обработка отклика без дублей', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended('/submissions/?source=form&recruiter=', [2019, 8, 22, 0, 0, 0], '/submissions/1')
            .disableAnimations('*')
            .waitForVisible(PO.submissionsTable.first())
            .assertView('submission_in_table', PO.submissionsTable.first())
            .staticElement(PO.sidePopup())
            .click(PO.submissionsTable.first.id())
            .waitForVisible(PO.fSubmission.actionHandle())
            .scroll(PO.fSubmission.actionHandle())
            .assertView('handle_button', PO.fSubmission.actionHandle())
            .click(PO.fSubmission.actionHandle())
            .waitForVisible(PO.fSubmissionWorkflowForm.header())
            .assertView('handle_form', PO.fSubmissionWorkflowForm())
            .click(PO.fSubmissionWorkflowForm.submit())
            .waitForHidden(PO.fSubmissionWorkflowForm())
            .assertView('handled_submission', PO.fSubmission())
            .assertView('handled_submission_in_table', PO.submissionsTable.first())
            .click(PO.submissionsTable.first.candidateLink())
            .assertUrl('/candidates/200015667');
    });
    it('Мерж к кандидату (отклик-рекомендация)', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended('/submissions/?source=reference&recruiter=', [2019, 8, 22, 0, 0, 0], '/submissions/1')
            .disableAnimations('*')
            .waitForVisible(PO.submissionsTable.first())
            .assertView('submission_in_table', PO.submissionsTable.first())
            .staticElement(PO.sidePopup())
            .click(PO.submissionsTable.first.id())
            .waitForVisible(PO.fSubmission.actionHandle())
            .scroll(PO.fSubmission.actionHandle())
            .assertView('handle_button', PO.fSubmission.actionHandle())
            .click(PO.fSubmission.actionHandle())
            .waitForVisible(PO.fSubmissionWorkflowForm.header())
            .assertView('handle_form_empty', PO.fSubmissionWorkflowForm())
            .click(PO.fSubmissionWorkflowForm.duplicatesField.first.radio())
            .pause(50) // Радиобатоны мигают
            .assertView('handle_form_filled', PO.fSubmissionWorkflowForm())
            .click(PO.fSubmissionWorkflowForm.submit())
            .waitForHidden(PO.fSubmissionWorkflowForm())
            .assertView('handled_submission', PO.fSubmission())
            .assertView('handled_submission_in_table', PO.submissionsTable.first())
            .click(PO.submissionsTable.first.candidateLink())
            .assertUrl('/candidates/200001134');
    });
    it('Обработка отклика-ротации', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended('/submissions/?source=rotation&recruiter=', [2019, 8, 22, 0, 0, 0], '/submissions/1')
            .disableAnimations('*')
            .waitForVisible(PO.submissionsTable.first())
            .assertView('submission_in_table', PO.submissionsTable.first())
            .staticElement(PO.sidePopup())
            .click(PO.submissionsTable.first.id())
            .waitForVisible(PO.fSubmission.actionHandle())
            .scroll(PO.fSubmission.actionHandle())
            .assertView('handle_button', PO.fSubmission.actionHandle())
            .click(PO.fSubmission.actionHandle())
            .waitForVisible(PO.fSubmissionWorkflowForm.header())
            .assertView('handle_form_empty', PO.fSubmissionWorkflowForm())
            .click(PO.fSubmissionWorkflowForm.duplicatesField.first.radio())
            .pause(50) // Радиобатоны мигают
            .assertView('handle_form_filled', PO.fSubmissionWorkflowForm())
            .click(PO.fSubmissionWorkflowForm.submit())
            .waitForHidden(PO.fSubmissionWorkflowForm())
            .assertView('handled_submission', PO.fSubmission())
            .assertView('handled_submission_in_table', PO.submissionsTable.first())
            .click(PO.submissionsTable.first.candidateLink())
            .assertUrl('/candidates/200015664');
    });
});
