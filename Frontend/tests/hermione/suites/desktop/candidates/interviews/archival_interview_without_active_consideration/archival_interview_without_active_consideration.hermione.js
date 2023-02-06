const PO = require('../../../../../page-objects/pages/candidate');

describe('Кандидат.Испытания / Архивное рассмотрение', function() {
    it('Внешний вид секций где все рассмотрения архивные', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/candidates/200015585/interviews/')
            .waitForVisible(PO.interviewsPane.firstArchivedConsideration())
            .assertView('archived_consideration_type_interview', PO.interviewsPane.archivedConsiderationsList());
    });
});
