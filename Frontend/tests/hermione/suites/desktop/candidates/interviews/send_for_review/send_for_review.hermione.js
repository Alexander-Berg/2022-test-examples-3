const { setAjaxHash } = require('../../../../../helpers');
const PO = require('../../../../../page-objects/pages/candidate');
const RPO = require('../../../../../page-objects/react-pages/candidate');

describe('Кандидат.Испытания / Отправка секции на ревью', function() {
    it('Проверка обязательных полей', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', 'candidates/200017031/interviews/')
            .waitForVisible(PO.interviewsPane.activeConsiderationsList.firstInterview())
            .assertView('interview', PO.interviewsPane.activeConsiderationsList.firstInterview())
            .click(PO.interviewsPane.activeConsiderationsList.firstInterview.actions.sendToReview())
            .waitForVisible(RPO.sendToReviewForm())
            .assertView('review_form', RPO.sendToReviewForm())
            .click(RPO.sendToReviewForm.submit())
            .waitForVisible(RPO.sendToReviewForm.formError())
            .assertView('form_error', RPO.sendToReviewForm());
    });
    it('Отправка секции на ревью', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended('candidates/200017031/interviews/', [2020, 12, 30, 0, 0, 0], '/applications/')
            .waitForVisible(PO.interviewsPane.activeConsiderationsList.firstInterview())
            .assertView('interview', PO.interviewsPane.activeConsiderationsList.firstInterview())
            .click(PO.interviewsPane.activeConsiderationsList.firstInterview.actions.sendToReview())
            .waitForVisible(RPO.sendToReviewForm())
            .assertView('review_form', RPO.sendToReviewForm())
            .setReactSFieldValue(RPO.sendToReviewForm.commentField(), 'Comment', 'textarea')
            .assertView('review_form_filled', RPO.sendToReviewForm())
            .execute(setAjaxHash, 'after_submit')
            .click(RPO.sendToReviewForm.submit())
            .waitForHidden(RPO.sendToReviewForm())
            .waitForVisible(PO.interviewsPane.activeConsiderationsList.firstInterview.firstComment())
            .assertView('interview_updated', PO.interviewsPane.activeConsiderationsList.firstInterview())
    });
});
