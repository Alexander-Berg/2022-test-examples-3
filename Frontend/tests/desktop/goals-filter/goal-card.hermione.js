const assert = require('chai').assert;

const PO = require('../../../page-objects');
const START_URL = '/filter?group=22&goal=47713';

describe('Фильтр целей', function() {
    beforeEach(function() {
        return this.browser
            .loginToGoals()
            .preparePage('filter-with-goal', START_URL)
            .waitForVisible(PO.goal.info());
    });

    it('должна открываться карточка, если в url указан id цели', function() {
        return this.browser
            .assertView('plain', PO.goal.info());
    });

    it('должна закрываться карточка при повторном клике на выбранную цель в списке', function() {
        return this.browser
            .waitForVisible(PO.goalsList.currentGoal())
            .click(PO.goalsList.currentGoal())
            .yaWaitForHidden(PO.goal.info())
            .assertView('list-with-hidden-card', PO.content());
    });

    it('при переходе назад должна появляться карточка цели', function() {
        return this.browser
            .waitForVisible(PO.goalsList.currentGoal())
            .click(PO.goalsList.currentGoal())
            .yaWaitForHidden(PO.goal.info())
            .back()
            .waitForVisible(PO.goal.info())
            .assertView('list-with-card', PO.content())
            .yaGetParsedUrl()
            .then(val => assert.equal(val.pathname + val.search, START_URL));
    });
});
