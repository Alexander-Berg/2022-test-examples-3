const PO = require('../../../page-objects');
const START_URL = '/resources/okr/52794';
const OKR_TREE_URL = {
    pathname: '/okr',
    searchParams: {
        importance: 'all',
        periods: '2021Q2,2021Q3',
        statuses: {
            type: 'array',
            value: '0,1,2,4,5,173,432,433,450,451',
        },
        selectedItem: '52794',
        goal: '52794',
    },
};

describe('Учет людей в ОКР', function() {
    beforeEach(function() {
        return this.browser
            .setViewportSize({ width: 2500, height: 2000 })
            .loginToGoals();
    });

    it('внешний вид', async function() {
        const browser = this.browser;

        await browser.preparePage('okr-resources-view', START_URL);

        await browser.waitForVisible(PO.resources.header(), 60000);
        await browser.assertView('header', PO.resources.header());
        await browser.assertView('user-cell', PO.resources.table.firstRow.userCell.content());
        await browser.assertView('goals-cell', PO.resources.table.firstRow.goalsCell());
    });

    it('кнопка дерева ведёт на ОКР', async function() {
        const browser = this.browser;

        await browser.preparePage('okr-resources-view', START_URL);

        await browser.waitForVisible(PO.resources.header.okrTreeButton(), 60000);
        await browser.yaCheckHref(PO.resources.header.okrTreeButton(), OKR_TREE_URL);
    });
});
