const assert = require('chai').assert;

const PO = require('../../../../page-objects');

describe('base-compilations', function() {
    beforeEach(function() {
        return this.browser.loginToGoals();
    });

    it('должно открывать базовую подборку пользователя', function() {
        return this.browser
            .preparePage('own-with-login', `/compilations/own?login=${process.env.YA_USER}`)
            .waitForVisible(PO.content.header())
            .assertView('own-compilation', PO.content.list());
    });

    it('должно открывать добавлять логин в url', function() {
        return this.browser
            .preparePage('own', '/compilations/own')
            .waitForVisible(PO.content.header())
            .yaGetParsedUrl()
            .then(val => assert.equal(val.search, '?login=user3993'))
            .assertView('own-compilation', PO.content.list());
    });

    it('должно редиректить на новый url базовой подборки', function() {
        return this.browser
            .preparePage('someone-own', '/compilations/11607?login=vasya')
            .waitForVisible(PO.content.header())
            .yaGetParsedUrl()
            .then(val => assert.equal(val.pathname + val.search, '/compilations/own?login=veged'))
            .assertView('compilation-filter', PO.content.list());
    });

    it('должно редиректить на фильтр, если подборка не базовая и от другого пользователя', function() {
        return this.browser
            .preparePage('someone-custom', '/compilations/114001?login=poalrom')
            .waitForVisible(PO.content.header())
            .yaGetParsedUrl()
            .then(val => assert.equal(val.pathname + val.search, '/filter?group=104058&status=435,433,434,432,0,1,2,173,5,0,1,2&importance=3,0,1,2,4'))
            .assertView('content', PO.content.list())
            // проверяем, что панель переключилась в стафф режим
            .assertView('navigation', PO.sidebar());
    });

    it('должно открывать карточку цели не меняя навигационную панель', function() {
        return this.browser
            .preparePage('own-with-goal', '/compilations/company?login=robot-goals&goal=33082')
            .waitForVisible(PO.goal.info())
            .waitForHidden(PO.spinner())
            .assertView('content', PO.content.list())
            // проверяем, что панель навигации не изменилась
            .assertView('navigation', PO.sidebar());
    });
});
