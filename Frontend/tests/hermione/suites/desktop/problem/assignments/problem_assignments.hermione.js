const PO = require('../../../../page-objects/pages/problem');

describe('Задача / Отображение вкладки ответов', function() {
    function login(browser) {
        return browser
            .conditionalLogin('marat');
    }
    it('Внешний вид пустой вкладки ответов', function() {
        return login(this.browser)
            .preparePage('', '/problems/3841/assignments/')
            .waitForVisible(PO.pageProblem.assignmetsList())
            .assertView('problem_assignments', PO.pageProblem.tabs());
    });
    it('Внешний вид одностраничной вкладки ответов', function() {
        return login(this.browser)
            .preparePage('', '/problems/4147/assignments/')
            .waitForVisible(PO.pageProblem.assignmetsList())
            .assertView('problem_assignments', PO.pageProblem.assignmetsList());
    });
    it('Внешний вид вкладки ответов с пагинацией', function() {
        return login(this.browser)
            .preparePage('', '/problems/116/assignments/')
            .waitForVisible(PO.pageProblem.assignmetsList())
            .assertView('problem_assignments', PO.pageProblem.assignmetsList());
    });
});
