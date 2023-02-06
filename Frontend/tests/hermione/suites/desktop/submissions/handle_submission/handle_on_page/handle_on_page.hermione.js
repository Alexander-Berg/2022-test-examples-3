const PO = require('../../../../../page-objects/pages/submission');

describe('Отклики / Обработка на странице', function() {
    it('Отправка в спам', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended('/submissions/1016/', [2019, 8, 22, 0, 0, 0], '/submissions/')
            .disableAnimations('*')
            .waitForVisible(PO.fSubmission.actionReject())
            .assertView('reject_button', PO.fSubmission.actionReject())
            .click(PO.fSubmission.actionReject())
            .waitForVisible(PO.fSubmissionWorkflowForm.header())
            .assertView('spam_form', PO.fSubmissionWorkflowForm())
            .click(PO.fSubmissionWorkflowForm.submit())
            .waitForHidden(PO.fSubmissionWorkflowForm(), 5000)
            .assertView('handled_submission', PO.fSubmission());
    });
    it('Cоздание кандидата', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended('/submissions/1018/', [2019, 8, 22, 0, 0, 0], '/submissions/')
            .disableAnimations('*')
            .waitForVisible(PO.fSubmission.actionHandle())
            .assertView('handle_button', PO.fSubmission.actionHandle())
            .click(PO.fSubmission.actionHandle())
            .waitForVisible(PO.fSubmissionWorkflowForm.header())
            .assertView('handle_form_empty', PO.fSubmissionWorkflowForm())
            .click(PO.fSubmissionWorkflowForm.createCandidate())
            .assertView('handle_form_filled', PO.fSubmissionWorkflowForm())
            .click(PO.fSubmissionWorkflowForm.submit())
            .waitForHidden(PO.fSubmissionWorkflowForm())
            .assertView('handled_submission', PO.fSubmission())
            .click(PO.fSubmission.candidate.link())
            .assertUrl('/candidates/200015660');
    });
    it('Мерж и отказ', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended('/submissions/1019/', [2019, 8, 22, 0, 0, 0], '/submissions/')
            .disableAnimations('*')
            .waitForVisible(PO.fSubmission.actionHandle())
            .assertView('handle_button', PO.fSubmission.actionHandle())
            .staticElement(PO.modal()) // Модалка слишком большая
            .click(PO.fSubmission.actionHandle())
            .waitForVisible(PO.fSubmissionWorkflowForm.header())
            .assertView('handle_form_empty', PO.fSubmissionWorkflowForm())
            .click(PO.fSubmissionWorkflowForm.duplicatesField.first.radio())
            .pause(50) // Радиобатоны мигают
            .click(PO.fSubmissionWorkflowForm.isRejectionField())
            .assertView('handle_form_filled', PO.fSubmissionWorkflowForm())
            .click(PO.fSubmissionWorkflowForm.submit())
            .waitForHidden(PO.fSubmissionWorkflowForm())
            .assertView('handled_submission', PO.fSubmission())
            .click(PO.fSubmission.candidate.link())
            .assertUrl('/candidates/200001129');
    });
    it('Отказ по отклику без дублей', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended('/submissions/1020/', [2019, 8, 22, 0, 0, 0], '/submissions/')
            .disableAnimations('*')
            .waitForVisible(PO.fSubmission.actionHandle())
            .assertView('handle_button', PO.fSubmission.actionHandle())
            .staticElement(PO.modal())
            .click(PO.fSubmission.actionHandle())
            .waitForVisible(PO.fSubmissionWorkflowForm.header())
            .assertView('handle_form_empty', PO.fSubmissionWorkflowForm())
            .click(PO.fSubmissionWorkflowForm.isRejectionField())
            .assertView('handle_form_filled', PO.fSubmissionWorkflowForm())
            .click(PO.fSubmissionWorkflowForm.submit())
            .waitForHidden(PO.fSubmissionWorkflowForm())
            .assertView('handled_submission', PO.fSubmission())
            .click(PO.fSubmission.candidate.link())
            .assertUrl('/candidates/200015661');
    });
    it('Создание кандидата (отклик-рекомендация)', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended('/submissions/1042/', [2019, 8, 22, 0, 0, 0], '/submissions/')
            .disableAnimations('*')
            .waitForVisible(PO.fSubmission.actionHandle())
            .assertView('handle_button', PO.fSubmission.actionHandle())
            .staticElement(PO.modal())
            .click(PO.fSubmission.actionHandle())
            .waitForVisible(PO.fSubmissionWorkflowForm.header())
            .assertView('handle_form_empty', PO.fSubmissionWorkflowForm())
            .click(PO.fSubmissionWorkflowForm.createCandidate())
            .pause(50) // Радиобатоны мигают
            .assertView('handle_form_filled', PO.fSubmissionWorkflowForm())
            .click(PO.fSubmissionWorkflowForm.submit())
            .waitForHidden(PO.fSubmissionWorkflowForm())
            .assertView('handled_submission', PO.fSubmission())
            .click(PO.fSubmission.candidate.link())
            .assertUrl('/candidates/200015663');
    });
    it('Обработка отклика-ротации', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended('/submissions/1045/', [2019, 8, 22, 0, 0, 0], '/submissions/')
            .disableAnimations('*')
            .waitForVisible(PO.fSubmission.actionHandle())
            .assertView('handle_button', PO.fSubmission.actionHandle())
            .staticElement(PO.modal())
            .click(PO.fSubmission.actionHandle())
            .waitForVisible(PO.fSubmissionWorkflowForm.header())
            .assertView('handle_form_empty', PO.fSubmissionWorkflowForm())
            .click(PO.fSubmissionWorkflowForm.duplicatesField.first.radio())
            .pause(50) // Радиобатоны мигают
            .assertView('handle_form_filled', PO.fSubmissionWorkflowForm())
            .click(PO.fSubmissionWorkflowForm.submit())
            .waitForHidden(PO.fSubmissionWorkflowForm())
            .assertView('handled_submission', PO.fSubmission())
            .click(PO.fSubmission.candidate.link())
            .assertUrl('/candidates/200015664');
    });
});
