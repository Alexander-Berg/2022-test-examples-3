const PO = require('../../../../../../page-objects/pages/candidate.js');

describe('Кандидат.Испытания / Кнопка серий секций', function() {
    it('Проверка кнопки создания', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/candidates/200017091/interviews/')
            .waitForVisible(PO.interviewsPane.activeConsiderationsList())
            .assertView('consideration_list_header', PO.interviewsPane.activeConsiderationsList.header())
            .click(PO.interviewsPane.activeConsiderationsList.createInterviewRound())
            .assertUrl('/candidates/200017091/interviews/create');
    });
    it('Проверка отсутствия кнопки', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/candidates/200017091/interviews/')
            .waitForVisible(PO.interviewsPane.archivedConsiderationsList())
            .assertView('consideration_list_header', PO.interviewsPane.archivedConsiderationsList.header());
    });
});
