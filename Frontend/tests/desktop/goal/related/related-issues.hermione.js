const PO = require('../../../../page-objects');
const START_URL = '/compilations/company?flags=goal_deps_tabs&goal=33564';

const DONT_MOVE = {
    dontMoveCursor: true,
};

describe('Задачи', function() {
    beforeEach(async function() {
        const browser = this.browser;

        await browser.setViewportSize({ width: 2500, height: 3000 });

        await browser.loginToGoals();

        await browser.preparePage('goal-related-issues', START_URL);
        await browser.waitForVisible(PO.relatedGoals());
        await browser.click(PO.relatedGoals.tasksHeader());
        await browser.waitForVisible(PO.relatedGoals.tasksContent.issue());
    });

    it('должен показываться список связанных задач и фильтров', async function() {
        const browser = this.browser;

        await browser.assertView('plain', PO.relatedGoals.tasksContent());
    });

    it('фильтр должен подгружать задачи', async function() {
        const browser = this.browser;

        await browser.yaShouldExist(PO.relatedGoals.tasksContent.filter.issue());
        await browser.click(PO.relatedGoals.tasksContent.filterExpandCollapse());
        await browser.waitForVisible(PO.relatedGoals.tasksContent.filter.issue());
        await browser.yaShouldNotExist(PO.relatedGoals.tasksContent.filter.secondPageIssue());

        await browser.waitForVisible(PO.relatedGoals.tasksContent.filter.showMoreButton());
        await browser.click(PO.relatedGoals.tasksContent.filter.showMoreButton());
        await browser.waitForVisible(PO.relatedGoals.tasksContent.filter.secondPageIssue());
    });

    it('удаление задач', async function() {
        const browser = this.browser;

        const before = await browser.yaCountElements(PO.relatedGoals.tasksContent.issue());
        const after = before - 1;

        await browser.moveToObject(PO.relatedGoals.tasksContent.firstIssue.status(), 10, 10);
        await browser.waitForVisible(PO.relatedGoals.tasksContent.firstIssue.deleteButton());
        await browser.assertView('delete-button', PO.relatedGoals.tasksContent.firstIssue.deleteButton(), DONT_MOVE);
        await browser.click(PO.relatedGoals.tasksContent.firstIssue.deleteButton());
        await browser.waitForVisible(PO.relatedGoals.tasksContent.firstIssue.deletePopup.confirmButton());
        await browser.assertView(
            'delete-issue-popup',
            PO.relatedGoals.tasksContent.firstIssue.deletePopup(),
            DONT_MOVE,
        );

        await browser.click(PO.relatedGoals.tasksContent.firstIssue.deletePopup.confirmButton());
        await browser.yaWaitUntilElCountChanged(PO.relatedGoals.tasksContent.issue(), after);
    });

    it('добавление задач', async function() {
        const browser = this.browser;

        const before = await browser.yaCountElements(PO.relatedGoals.tasksContent.issue());
        const after = before + 1;

        await browser.click(PO.relatedGoals.tasksContent.addIssueButton());
        await browser.waitForVisible(PO.relatedGoals.tasksContent.editForm());
        const input = browser.$(PO.relatedGoals.tasksContent.editForm.input());

        await input.setValue('expired goal');
        await browser.waitForVisible(PO.relatedGoals.tasksContent.editForm.suggestChoice());
        await browser.assertView('issue-edit', PO.relatedGoals.tasksContent.editForm());
        await browser.click(PO.relatedGoals.tasksContent.editForm.suggestChoice());
        await browser.click(PO.relatedGoals.tasksContent.editForm.buttons.save());
        await browser.yaWaitUntilElCountChanged(PO.relatedGoals.tasksContent.issue(), after);
    });
});
