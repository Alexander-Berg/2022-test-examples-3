const PO = require('../../../page-objects');
const START_URL = '/resources/staff/58350';
const GOAL_URL = {
    pathname: '/okr',
    searchParams: {
        importance: 'all',
        periods: '2021Q2,2021Q3',
        statuses: {
            type: 'array',
            value: '0,1,2,4,5,173,432,433,450,451',
        },
        selectedItem: '52394',
        goal: '52394',
    },
};

const STAFF_URL = {
    pathname: '/departments/yandex_search_interface_service_innopolis',
};

describe('Учет людей в staff', function() {
    beforeEach(function() {
        return this.browser
            .setViewportSize({ width: 2500, height: 2000 })
            .loginToGoals();
    });

    it('внешний вид', async function() {
        const browser = this.browser;

        await browser.preparePage('okr-resources-view', START_URL);

        await browser.waitForVisible(PO.staffResources.header());
        await browser.assertView('header', PO.staffResources.header());
        await browser.assertView('user-cell', PO.staffResources.table.firstRow.userCell.content());
        await browser.assertView('goals-cell', PO.staffResources.table.firstRow.goalsCell());
    });

    it('проверка ссылок', async function() {
        const browser = this.browser;

        await browser.preparePage('okr-resources-view', START_URL);

        await browser.waitForVisible(PO.staffResources.table.firstRow.goalsCell.goalLink());
        await browser.yaCheckHref(PO.staffResources.table.firstRow.goalsCell.goalLink(), GOAL_URL);
        await browser.yaCheckHref(PO.staffResources.table.firstRow.staffCell.staffLink(), STAFF_URL);
    });
});
