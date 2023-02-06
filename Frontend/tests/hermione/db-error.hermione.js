const assert = require('chai').assert;

describe('db-error:', function() {
    afterEach(function() {
        return this.browser.yaCheckClientErrors();
    });

    it('ошибка соединения с базой данных', function() {
        return this.browser
            .openSbsWithDbError('/experiments')
            .waitForExist('.ErrorPage-Title', 5000)
            .getText('.ErrorPage-Title')
            .then((title) => assert.equal(title, '503. Ошибка соединения с базой данных'));
    });
});
