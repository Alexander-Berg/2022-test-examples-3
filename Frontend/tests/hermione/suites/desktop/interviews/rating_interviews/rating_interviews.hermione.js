const PO = require('../../../../page-objects/pages/interview');

describe('Секция / Оценка секции', function() {
    it('Оценка секции при найме', function() {
        const URL = '/interviews/32751/';

        return this.browser
            .conditionalLogin('marat')
            .preparePage('', URL)
            .waitForVisible(PO.interviewFinish())
            .patchStyle(PO.tabs.panes(), { position: 'relative' })
            .assertView('actions', PO.interviewPage.actions())
            .assertView('grade', PO.interviewPage.grade())
            .assertView('finish_button', PO.interviewFinish())
            .click(PO.interviewPage.grade.hire3())
            // В 5-й версии это будет acceptAlert()
            .alertAccept()
            .waitForHidden(PO.interviewPage.grade.hire3())
            .assertView('interview_assessed', PO.interviewPage());
    });
    it('Оценка секции при отказе в найме', function() {
        const URL = '/interviews/32753/';

        return this.browser
            .conditionalLogin('marat')
            .preparePage('', URL)
            .waitForVisible(PO.interviewFinish())
            .patchStyle(PO.tabs.panes(), { position: 'relative' })
            .assertView('actions', PO.interviewPage.actions())
            .assertView('grade', PO.interviewPage.grade())
            .assertView('finish_button', PO.interviewFinish())
            .click(PO.interviewPage.grade.nohire())
            // В 5-й версии это будет acceptAlert()
            .alertAccept()
            .waitForHidden(PO.interviewPage.grade.nohire())
            .assertView('interview_assessed', PO.interviewPage());
    });
});
