const PO = require('../../../../page-objects');
const START_URL = '/compilations/company?flags=goal_deps_tabs&goal=33564';

describe('Критерии', function() {
    beforeEach(async function() {
        const browser = this.browser;

        await browser.setViewportSize({ width: 2500, height: 3000 });
        await browser.loginToGoals();
        await browser.preparePage('goal-related-criterias', START_URL);
        await browser.waitForVisible(PO.relatedGoals());
        await browser.click(PO.relatedGoals.criteriasMetricsHeader());
        await browser.waitForVisible(PO.relatedGoals.criteriasContent.criteria());
    });

    it('должен показываться список критериев', async function() {
        const browser = this.browser;

        await browser.assertView('criterias-list', PO.relatedGoals.criteriasContent());
    });

    it('должен отмечать/снимать отметку по критериям', async function() {
        const browser = this.browser;

        const checked = await browser.yaCountElements(PO.relatedGoals.criteriasContent.criteria.checked());

        //check
        await browser.click(PO.relatedGoals.criteriasContent.criteria.checkbox());
        await browser.yaWaitUntilElCountChanged(PO.relatedGoals.criteriasContent.criteria.checked(), checked + 1);
        await browser.assertView('first-checked', PO.relatedGoals.criteriasContent());
    });

    it('должен добавлять критерий', async function() {
        const browser = this.browser;

        const before = await browser.yaCountElements(PO.relatedGoals.criteriasContent.criteria());
        const after = before + 1;

        await browser.click(PO.relatedGoals.criteriasContent.addButton());
        await browser.waitForVisible(PO.relatedGoals.criteriasContent.editForm());
        await browser.assertView('empty-edit-criteria-form', PO.relatedGoals.criteriasContent.editForm());

        const input = browser.$(PO.relatedGoals.criteriasContent.editForm.input());

        await input.setValue('добавляем новый критерий');
        await browser.click(PO.relatedGoals.criteriasContent.editForm.deadlineSelect());
        await browser.waitForVisible(PO.relatedGoals.criteriasContent.editForm.deadlineSelect.popup());
        await browser.click(PO.relatedGoals.criteriasContent.editForm.deadlineSelect.popup.firstItem());
        await browser.waitForHidden(PO.relatedGoals.criteriasContent.editForm.deadlineSelect.popup());
        await browser.click(PO.relatedGoals.criteriasContent.editForm.addAssigneeButton());
        await browser.waitForVisible(PO.relatedGoals.criteriasContent.editForm.assigneeInput());
        await browser.click(PO.relatedGoals.criteriasContent.editForm.assigneeInput());

        const assigneeInput = browser.$(PO.relatedGoals.criteriasContent.editForm.assigneeInput.input());

        await assigneeInput.setValue('Bulat Safin');

        await browser.waitForVisible(PO.relatedGoals.criteriasContent.editForm.assigneeInput.popup());
        await browser.click(PO.relatedGoals.criteriasContent.editForm.assigneeInput.popup.firstItem());
        await browser.waitForHidden(PO.relatedGoals.criteriasContent.editForm.assigneeInput.popup());
        await browser.assertView('filled-form', PO.relatedGoals.criteriasContent.editForm());

        await browser.click(PO.relatedGoals.criteriasContent.editForm.buttons.save());
        await browser.yaWaitUntilElCountChanged(PO.relatedGoals.criteriasContent.criteria(), after);
    });

    it('должен удалять критерий', async function() {
        const browser = this.browser;

        const before = await browser.yaCountElements(PO.relatedGoals.criteriasContent.criteria());
        const after = before - 1;

        await browser.moveToObject(PO.relatedGoals.criteriasContent.criteria.checkbox(), 2, 2);
        await browser.waitForVisible(PO.relatedGoals.criteriasContent.criteria.deleteButton());
        await browser.click(PO.relatedGoals.criteriasContent.criteria.deleteButton());
        await browser.waitForVisible(PO.relatedGoals.criteriasContent.criteria.deletePopup());
        await browser.click(PO.relatedGoals.criteriasContent.criteria.deletePopup.confirmButton());

        await browser.yaWaitUntilElCountChanged(PO.relatedGoals.criteriasContent.criteria(), after);
    });

    it('должен редактировать критерий', async function() {
        const browser = this.browser;

        await browser.moveToObject(PO.relatedGoals.criteriasContent.secondCriteria.checkbox(), 2, 2);
        await browser.waitForVisible(PO.relatedGoals.criteriasContent.secondCriteria.editButton());

        await browser.click(PO.relatedGoals.criteriasContent.secondCriteria.editButton());
        await browser.waitForVisible(PO.relatedGoals.criteriasContent.editForm());

        const input = browser.$(PO.relatedGoals.criteriasContent.editForm.input());

        await input.setValue('отредактированный критерий');
        await browser.click(PO.relatedGoals.criteriasContent.editForm.deadlineSelect());
        await browser.waitForVisible(PO.relatedGoals.criteriasContent.editForm.deadlineSelect.popup());
        await browser.click(PO.relatedGoals.criteriasContent.editForm.deadlineSelect.popup.secondItem());
        await browser.waitForHidden(PO.relatedGoals.criteriasContent.editForm.deadlineSelect.popup());
        await browser.click(PO.relatedGoals.criteriasContent.editForm.addAssigneeButton());
        await browser.waitForVisible(PO.relatedGoals.criteriasContent.editForm.assigneeInput());
        await browser.click(PO.relatedGoals.criteriasContent.editForm.assigneeInput());

        const assigneeInput = browser.$(PO.relatedGoals.criteriasContent.editForm.assigneeInput.input());

        await assigneeInput.setValue('Джейн Доу');

        await browser.waitForVisible(PO.relatedGoals.criteriasContent.editForm.assigneeInput.popup());
        await browser.click(PO.relatedGoals.criteriasContent.editForm.assigneeInput.popup.firstItem());
        await browser.waitForHidden(PO.relatedGoals.criteriasContent.editForm.assigneeInput.popup());
        await browser.assertView('edit-criteria-edited', PO.relatedGoals.criteriasContent.editForm());

        await browser.click(PO.relatedGoals.criteriasContent.editForm.buttons.save());
        await browser.waitForHidden(PO.relatedGoals.criteriasContent.editForm());
    });
});
