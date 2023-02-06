const { setAjaxHash } = require('../../../../helpers');
const PO = require('../../../../page-objects/pages/interview');

describe('Секция / Отправка секции на ревью', function() {
    it('Проверка обязательных полей', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/interviews/33761/')
            .waitForVisible(PO.interviewPage.actions())
            .waitForVisible(PO.interviewPage.actions.sendToReview.icon())  //Ждем пока иконка жучка загрузится
            .assertView('actions', PO.interviewPage.actions())
            .click(PO.interviewPage.actions.sendToReview())
            .waitForVisible(PO.sendToReviewForm())
            .assertView('review_form', PO.sendToReviewForm())
            .click(PO.sendToReviewForm.submit())
            .waitForVisible(PO.sendToReviewForm.formError())
            .assertView('form_error', PO.sendToReviewForm());
    });
    it('Отправка секции на ревью', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended(`/interviews/33761/`, [2020, 12, 30, 0, 0, 0], '/applications/')
            .waitForVisible(PO.interviewPage.actions())
            .waitForVisible(PO.interviewPage.actions.sendToReview.icon())
            .assertView('actions', PO.interviewPage.actions())
            .click(PO.interviewPage.actions.sendToReview())
            .waitForVisible(PO.sendToReviewForm())
            .assertView('review_form', PO.sendToReviewForm())
            .setReactSFieldValue(PO.sendToReviewForm.commentField(), 'Comment', 'textarea')
            .assertView('review_form_filled', PO.sendToReviewForm())
            .execute(setAjaxHash, 'after_submit')
            .click(PO.sendToReviewForm.submit())
            .waitForHidden(PO.sendToReviewForm())
            .waitForVisible(PO.interviewPage.comments.list.first())
            .assertView('info_updated', PO.interviewPage.info())
            .assertView('comments_list_updated', PO.interviewPage.comments.list());
    });
});
