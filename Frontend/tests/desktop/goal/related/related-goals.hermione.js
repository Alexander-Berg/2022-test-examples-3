const assert = require('chai').assert;

const PO = require('../../../../page-objects');
const START_URL = '/compilations/company?goal=52789';
const EMPTY_RELATIONS_URL = '/compilations/company?goal=58511';

describe('Связанные цели', function() {
    beforeEach(async function() {
        const browser = this.browser;

        await browser.setViewportSize({ width: 2500, height: 2000 });

        await browser.loginToGoals();
    });

    it('должен показываться список дочерних, блокирующих и связанных целей', async function() {
        const browser = this.browser;

        await browser.preparePage('goal-relations', START_URL);
        await browser.waitForVisible(PO.goalTabControl.relations());
        await browser.click(PO.goalTabControl.relations());
        await browser.waitForVisible(PO.relations.group());
        await browser.assertView('plain', PO.relatedGoals());

        await browser.click(PO.relations.addButton());
        await browser.assertView('addForm', PO.relations.addForm());

        await browser.yaSelectChooseItem(PO.relations.addForm.typeSelect(), 'Блокирует');
        await browser.assertView('addFormWithActiveSuggest', PO.relations.addForm());

        await browser.moveToObject(PO.relations.item());
        await browser.assertView('hovered', PO.relations.item(), {
            dontMoveCursor: true,
        });

        await browser.moveToObject(PO.relations.item.deleteButton());
        await browser.assertView('deleteButtonHint', PO.relations.item.deleteHint(), {
            dontMoveCursor: true,
        });

        await browser.click(PO.relations.item.deleteButton());
        await browser.assertView('deleteConfirmationPopup', PO.relations.item.deleteConfirmationPopup(), {
            dontMoveCursor: true,
        });

        await browser.moveToObject(PO.relations.item.changeTypeButton());
        await browser.assertView('changeTypeButtonHint', PO.relations.item.changeTypeHint(), {
            dontMoveCursor: true,
        });

        await browser.click(PO.relations.item.changeTypeButton());
        await browser.assertView('changeTypeMenu', PO.relations.item.changeTypeMenu(), {
            dontMoveCursor: true,
        });
    });

    // что то с тестом, локально не воспроизводится
    // проблема только в гермионе
    // eslint-disable-next-line
    it.skip('связи должны обновляться при смене цели', async function() {
        const browser = this.browser;

        await browser.preparePage('goal-relations', START_URL);
        await browser.waitForVisible(PO.goalTabControl.relations());
        await browser.click(PO.goalTabControl.relations());
        await browser.waitForVisible(PO.relations.group());

        const subgoalsCount = (await browser.$$(PO.relations.subtasksGroup.node())).length;

        await browser.click(PO.relations.subtasksGroup.node.text());
        await browser.waitForVisible(PO.goalTabControl.relations());
        await browser.click(PO.goalTabControl.relations());
        await browser.waitForVisible(PO.relations.subtasksGroup.node());

        const updatedSubgoalsCount = (await browser.$$(PO.relations.subtasksGroup.node())).length;

        assert.equal(updatedSubgoalsCount, 0, 'Связанные цели должны обновиться');

        await browser.back();
        await browser.waitForVisible(PO.goalTabControl.relations());
        await browser.click(PO.goalTabControl.relations());
        await browser.waitForVisible(PO.relations.group());

        const originalSubgoalsCount = (await browser.$$(PO.relations.subtasksGroup.node())).length;

        assert.equal(subgoalsCount, originalSubgoalsCount, 'При переходе назад связанные цели должны обновиться');
    });

    // Падает с 500 - {"errors":{},"errorMessages":["VALIDATION_FAILED"],"statusCode":500}
    // eslint-disable-next-line
    it.skip('связь можно создать, изменить и удалить', async function() {
        const browser = this.browser;

        await browser.preparePage('goal-relations', START_URL);
        await browser.waitForVisible(PO.goalTabControl.relations());
        await browser.click(PO.goalTabControl.relations());
        await browser.waitForVisible(PO.relations.dependsGroup());

        const subtasksCount = (await browser.$$(PO.relations.subtasksGroup.node())).length;
        const dependsCount = (await browser.$$(PO.relations.dependsGroup.node())).length;

        // Создание работает
        await browser.click(PO.relations.addButton());
        await browser.yaSelectChooseItem(PO.relations.addForm.typeSelect(), 'Дочерняя');
        await browser.yaWaitForHidden(PO.relations.addForm.goalSuggestDisabled(), 'Саджест должен разблокироваться');
        await browser.yaSuggestChooseItem(
            PO.relations.addForm.goalSuggest(),
            'зонт для прикрепл',
            'Зонт для прикрепления в автотестах',
        );
        await browser.yaWaitForHidden(PO.relations.addForm.saveButtonDisabled());
        await browser.click(PO.relations.addForm.saveButton());
        await browser.yaWaitUntilElCountChanged(PO.relations.subtasksGroup.node(), subtasksCount + 1);
        await browser.yaShouldBeVisible(PO.relations.subtasksGroup.createdNode());

        // Перемещение работает
        await browser.moveToObject(PO.relations.subtasksGroup.createdNode());
        await browser.yaSelectChooseItem(
            PO.relations.subtasksGroup.createdNode.changeTypeButton(),
            'Блокирует',
        );
        await browser.yaWaitForHidden(PO.relations.subtasksGroup.createdNode(), 'Связь должна исчезнуть из дочерних');
        await browser.waitForVisible(PO.relations.dependsGroup.movedNode(), 'Связь должна добавиться в блокирующие');

        // Можно отменить удаление
        await browser.moveToObject(PO.relations.dependsGroup.movedNode());
        await browser.click(PO.relations.dependsGroup.movedNode.deleteButton());
        await browser.click(PO.relations.dependsGroup.movedNode.deleteConfirmationPopup.rejectButton());
        await browser.yaShouldBeVisible(PO.relations.dependsGroup.movedNode());

        // Удаление работает
        await browser.moveToObject(PO.relations.dependsGroup.movedNode());
        await browser.click(PO.relations.dependsGroup.movedNode.deleteButton());
        await browser.click(PO.relations.dependsGroup.movedNode.deleteConfirmationPopup.confirmButton());
        await browser.yaWaitUntilElCountChanged(PO.relations.dependsGroup.node(), dependsCount);
        await browser.yaShouldBeVisible(PO.relations.dependsGroup.movedNode(), 'Связь должна удалиться', false);
    });

    it('при смене типа связи в форме создания показывается ошибка, если она не подходит по типу', async function() {
        const browser = this.browser;

        await browser.preparePage('goal-relations', START_URL);
        await browser.waitForVisible(PO.goalTabControl.relations());

        await browser.click(PO.goalTabControl.relations());
        await browser.waitForVisible(PO.relations.dependsGroup());

        await browser.click(PO.relations.addButton());
        await browser.yaSelectChooseItem(PO.relations.addForm.typeSelect(), 'Блокирует');
        await browser.yaWaitForHidden(PO.relations.addForm.goalSuggestDisabled(), 'Саджест должен разблокироваться');
        await browser.yaSuggestChooseItem(
            PO.relations.addForm.goalSuggest(),
            'VS для прикрепления',
            'VS для прикрепления в автотестах',
        );
        await browser.yaShouldBeVisible(PO.relations.addForm.goalSuggest.choice());
        await browser.yaSelectChooseItem(PO.relations.addForm.typeSelect(), 'Дочерняя');
        await browser.yaShouldBeVisible(PO.relations.addForm.error(), 'Нельзя сделать Value stream дочерним элементом');
    });

    it('клик по кнопке дерева открывает дерево', async function() {
        const browser = this.browser;

        await browser.preparePage('goal-tree-button', START_URL);
        await browser.waitForVisible(PO.relatedGoals.treeButton());
        await browser.assertView('plain', PO.relatedGoals.treeButton());
        await browser.click(PO.relatedGoals.treeButton());
        await browser.waitForVisible(PO.goalsTree());
    });

    it('кнопка дерева не отображается в цели без связей', async function() {
        const browser = this.browser;

        await browser.preparePage('goal-empty-relations', EMPTY_RELATIONS_URL);
        await browser.yaShouldExist(PO.relatedGoals.treeButton(), 'кнопки дерева не должно быть в цели без связей', false);
    });
});
