const PO = require('../../../../page-objects/pages/interview');

describe('Секция / Завершение секции', function() {
    it('Завершение секции', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/interviews/32073/')
            .waitForVisible(PO.interviewFinish())
            .assertView('finish_button', PO.interviewFinish())
            .waitForHidden(PO.event.spin())
            .click(PO.interviewComment.comment())
            .waitForVisible(PO.interviewComment.comment.control())
            .setValue(PO.interviewComment.comment.control(), '123')
            .click(PO.interviewComment.buttons.save())
            .waitForHidden(PO.interviewComment.comment())
            .pause(100)
            .click(PO.interviewFinish())
            .waitForHidden(PO.interviewFinish())
            .click(PO.interviewPage.header())
            .waitForVisible(PO.interviewPage.assigned.list())
            .assertView('interview_finished', PO.interviewPage());
    });
});
