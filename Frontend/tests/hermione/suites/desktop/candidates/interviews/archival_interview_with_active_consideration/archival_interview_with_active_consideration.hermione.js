const PO = require('../../../../../page-objects/pages/candidate');

describe('Кандидат.Испытания / Активное и архивное рассмотрение', function() {
    it('Внешний вид секций с активными и архивными рассмотрениями', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/candidates/200015585/interviews/')
            .waitForVisible(PO.interviewsPane.firstArchivedConsideration())
            .assertView('archived_consideration_type_interview', PO.interviewsPane.archivedConsiderationsList())
            .click(PO.interviewsPane.firstArchivedConsideration.cat())
            .waitForVisible(PO.interviewsPane.firstArchivedConsideration.firstInterview())
            .assertView('archived_consideration_type_interview_expanded', PO.interviewsPane.archivedConsiderationsList());
    });
});
