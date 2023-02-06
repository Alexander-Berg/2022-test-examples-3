const PO = require('../../../../page-objects');

describe('Отображение цели', function() {
    it('Внешний вид резолюции', function() {
        return this.browser
            .loginToGoals()
            .preparePage('view', '/compilations/own?login=user3993&goal=58193')
            .waitForVisible(PO.goal.info.goalResolution())
            .assertView('resolution', PO.goal.info.goalResolution());
    });
});
