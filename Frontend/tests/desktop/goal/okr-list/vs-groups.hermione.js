const PO = require('../../../../page-objects');

describe('Группы VS', function() {
    beforeEach(async function() {
        const browser = this.browser;

        await browser.setViewportSize({ width: 2500, height: 3000 });
        await browser.loginToGoals();
        await browser.preparePage('main', '/');
        await browser.waitForVisible(PO.sidebar.navigation());
        await browser.click(PO.sidebar.navigation.okr());
        await browser.waitForVisible(PO.okrStructure.tree.node());
    });

    // eslint-disable-next-line
    it.skip('должен загружать список', async function() {
        const browser = this.browser;

        await browser.assertView('okr-list', PO.sidebar.navigation());
    });

    // eslint-disable-next-line
    it.skip('должен фильтровать по тегам', async function() {
        const browser = this.browser;

        await browser.yaSuggestChooseItem(
            PO.okrStructure.tagSuggestField(),
            PO.okrStructure.tagSuggestField.suggest(),
            'fin',
            'fintech',
        );

        await browser.yaWaitUntilElCountChanged(PO.okrStructure.tree.node(), 7);
        await browser.assertView('okr-filtered-list', PO.okrStructure());
    });

    // eslint-disable-next-line
    it.skip('должен отображать семестры и несколько vs', async function() {
        const browser = this.browser;

        //Xpath - жесть, как она есть
        await browser.click('//div[contains(@class, "OkrStructureNode")]/div/a[contains(@href, "57447")]/../..');

        await browser.waitForVisible(PO.newGoalsTree());
        await browser.waitForVisible(PO.newGoalsTreeFilter());

        await browser.assertView('goals-tree', PO.goalsTree());
    });

    // eslint-disable-next-line
    it.skip('должен фильтровать по разным семестрам, статусам и звездным целям', async function() {
        const browser = this.browser;

        await browser.click('//div[contains(@class, "OkrStructureNode")]/div/a[contains(@href, "57447")]/../..');

        await browser.waitForVisible(PO.newGoalsTreeFilter());
        await browser.click(PO.newGoalsTreeFilter.semesterSelector.firstSemester());

        await browser.waitForVisible(PO.newGoalsTree.HighlightedGoalsNode());
        await browser.assertView('semester-changed', PO.goalsTree());

        await browser.click(PO.newGoalsTreeFilter.buttonExpand());
        await browser.waitForVisible(PO.newGoalsTreeFilter.checkStarredOnly());

        await browser.assertView('filter-expanded', PO.newGoalsTreeFilter());

        await browser.yaSelectClickItem(PO.newGoalsTreeFilter.statuses(), 'По плану', 'Отменена', 'Достигнута');
        await browser.waitForVisible(PO.newGoalsTree.goalsNode());

        await browser.assertView('status-changed', PO.goalsTree());

        await browser.click('.GoalsNode[data-id="57048"] .GoalsNode-Expand');
        await browser.assertView('node-expanded', PO.goalsTree());
        await browser.click(PO.newGoalsTreeFilter.checkStarredOnly());
        await browser.assertView('node-expanded-starred-only', PO.goalsTree());
    });
});
