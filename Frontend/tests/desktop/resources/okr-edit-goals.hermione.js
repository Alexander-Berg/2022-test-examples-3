const PO = require('../../../page-objects');
const START_URL = '/resources/okr/57044';
const START_EDIT_MAIN_UMB_URL = '/resources/okr/52794';

describe('Учет людей в ОКР. Редактирование целей', function() {
    beforeEach(function() {
        return this.browser
            .setViewportSize({ width: 2500, height: 2000 })
            .loginToGoals();
    });

    it('Добавление цели', async function() {
        const browser = this.browser;

        await browser.preparePage('okr-resources-goal-edit', START_URL);

        await browser.waitForVisible(PO.resources.table.secondRow.goalsCell.goal());

        const actualGoalsCount = await browser.yaCountElements(
            PO.resources.table.secondRow.goalsCell.goal(),
        );

        await browser.moveToObject(PO.resources.table.secondRow.goalsCell(), 10, 10);
        await browser.waitForVisible(PO.resources.table.secondRow.goalsCell.editGoalButton());
        await browser.click(PO.resources.table.secondRow.goalsCell.editGoalButton());
        await browser.waitForVisible(PO.resources.table.secondRow.goalsCell.editButtons());
        await browser.assertView('empty-edit', PO.resources.table.secondRow.goalsCell());

        await browser.click(PO.resources.table.secondRow.goalsCell.editButtons.addGoalButton());
        await browser.waitForVisible(PO.resources.table.secondRow.goalsCell.goalEditor());
        await browser.assertView('new-goal-edit', PO.resources.table.secondRow.goalsCell());

        await browser.yaSuggestChooseItem(
            PO.resources.table.secondRow.goalsCell.goalEditor.goalSuggest(),
            PO.resources.table.secondRow.goalsCell.goalEditor.goalSuggestPopup(),
            'contour 104',
            'contour 104');

        await browser.assertView('new-goal-selected', PO.resources.table.secondRow.goalsCell());

        await browser.click(PO.resources.table.secondRow.goalsCell.goalEditor.percentEditor());
        await browser.yaKeyPress('BACKSPACE');
        await browser.yaKeyPress('12');

        await browser.assertView('new-goal-percent-edited', PO.resources.table.secondRow.goalsCell());

        await browser.click(PO.resources.table.secondRow.goalsCell.editButtons.save());
        await browser.waitForVisible(PO.resources.table.secondRow.goalsCell.goalListItem());
        await browser.yaWaitUntilElCountChanged(
            PO.resources.table.secondRow.goalsCell.goal(),
            val => val > actualGoalsCount,
        );
        await browser.assertView('new-goal-added', PO.resources.table.secondRow.goalsCell());
    });

    it('редактирование целей', async function() {
        const browser = this.browser;

        await browser.preparePage('okr-resources-goal-edit', START_URL);

        await browser.waitForVisible(PO.resources.table.firstRow.goalsCell());
        await browser.moveToObject(PO.resources.table.firstRow.goalsCell(), 10, 10);
        await browser.waitForVisible(PO.resources.table.firstRow.goalsCell.editGoalButton());
        await browser.click(PO.resources.table.firstRow.goalsCell.editGoalButton());
        await browser.waitForVisible(PO.resources.table.firstRow.goalsCell.editButtons());

        await browser.assertView('initial-state', PO.resources.table.firstRow.goalsCell());

        await browser.click(PO.resources.table.firstRow.goalsCell.goalEditor.goalSuggestClear());

        await browser.yaSuggestChooseItem(
            PO.resources.table.firstRow.goalsCell.goalEditor.goalSuggest(),
            PO.resources.table.firstRow.goalsCell.goalEditor.goalSuggestPopup(),
            'coun',
            'countur 107');

        await browser.click(PO.resources.table.firstRow.goalsCell.goalEditor.percentEditor());
        await browser.yaKeyPress('BACKSPACE');
        await browser.yaKeyPress('13');
        await browser.assertView('changed-first-goal', PO.resources.table.firstRow.goalsCell());

        await browser.click(PO.resources.table.firstRow.goalsCell.secondGoalEditor.deleteButton());
        await browser.assertView('second-goal-deleted', PO.resources.table.firstRow.goalsCell());

        await browser.click(PO.resources.table.firstRow.goalsCell.editButtons.save());
        await browser.waitForVisible(PO.resources.table.firstRow.goalsCell.goalListItem.percent());
        await browser.yaWaitUntilElCountChanged(PO.resources.table.firstRow.goalsCell.goalListItem.percent(), 2);
        await browser.assertView('goals-edited', PO.resources.table.firstRow.goalsCell());
    });

    it('редактирование основного зонта', async function() {
        const browser = this.browser;
        const umbName = 'Много людей';

        await browser.preparePage('okr-resources-main-umb-edit', START_EDIT_MAIN_UMB_URL);

        await browser.waitForVisible(PO.resources.table.firstRow.mainUmbrellaCell(), 60000);
        await browser.moveToObject(PO.resources.table.firstRow.mainUmbrellaCell(), 10, 10);
        await browser.waitForVisible(PO.resources.table.firstRow.mainUmbrellaCell.editGoalButton());
        await browser.click(PO.resources.table.firstRow.mainUmbrellaCell.editGoalButton());
        await browser.waitForVisible(PO.resources.table.firstRow.mainUmbrellaCell.editButtons());

        // await browser.click(PO.resources.table.firstRow.mainUmbrellaCell.goalSuggestClear());

        await browser.yaSuggestChooseItem(
            PO.resources.table.firstRow.mainUmbrellaCell.goalSuggest(),
            PO.resources.table.firstRow.mainUmbrellaCell.goalSuggestPopup(),
            'много',
            umbName);

        await browser.assertView('changed-main-umbrella', PO.resources.table.firstRow.mainUmbrellaCell());
        await browser.click(PO.resources.table.firstRow.mainUmbrellaCell.editButtons.save());
        await browser.waitForVisible(PO.resources.table.firstRow.mainUmbrellaCell.title());
        await browser.yaWaitUntil('Заголовок должен появиться', async() => {
            const mainUmbName = await browser.getText(PO.resources.table.firstRow.mainUmbrellaCell.title());

            return mainUmbName === umbName;
        });
        await browser.assertView('changed-main-umbrella-saved', PO.resources.table.firstRow.mainUmbrellaCell());

        await browser.moveToObject(PO.resources.table.firstRow.mainUmbrellaCell(), 10, 10);
        await browser.waitForVisible(PO.resources.table.firstRow.mainUmbrellaCell.editGoalButton());
        await browser.click(PO.resources.table.firstRow.mainUmbrellaCell.editGoalButton());
        await browser.waitForVisible(PO.resources.table.firstRow.mainUmbrellaCell.editButtons());

        await browser.click(PO.resources.table.firstRow.mainUmbrellaCell.goalSuggestClear());

        await browser.assertView('removed-main-umbrella', PO.resources.table.firstRow.mainUmbrellaCell());
        await browser.click(PO.resources.table.firstRow.mainUmbrellaCell.editButtons.save());
        await browser.waitForHidden(PO.resources.table.firstRow.mainUmbrellaCell.editButtons());
        await browser.assertView('removed-main-umbrella-saved', PO.resources.table.firstRow.mainUmbrellaCell());
    });
});
