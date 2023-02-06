const PO = require('../../../../page-objects/pages/submission');
const options = {
    tolerance: 5,
    antialiasingTolerance: 5,
}

describe('Отклики / Отображение страницы', function() {
    it('Внешний вид нового отклика', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/submissions/992')
            .waitForVisible(PO.fSubmission.header())
            .waitForPageLoad()
            .assertView('submission', PO.fSubmission(), options);
    });
    it('Внешний вид отклика, обработанного как спам', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/submissions/993')
            .waitForVisible(PO.fSubmission.header())
            .waitForPageLoad()
            .assertView('submission', PO.fSubmission(), options);
    });
    it('Внешний вид отклика, прикрепленного к кандидату', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/submissions/994')
            .waitForVisible(PO.fSubmission.header())
            .waitForPageLoad()
            .assertView('submission', PO.fSubmission(), options);
    });
});
