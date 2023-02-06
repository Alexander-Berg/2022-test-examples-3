const assert = require('chai').assert;

const PO = require('../../../page-objects');
const START_URL = '/creative-mode/57452';
const ROBOT_NAME = 'Марат Январев';

describe('Режим креатива', function() {
    beforeEach(function() {
        return this.browser
            .setViewportSize({ width: 2500, height: 2000 })
            .loginToGoals();
    });

    it('внешний вид шапки', function() {
        return this.browser
            .preparePage('creative-mode', START_URL + '?comment')
            .waitForVisible(PO.creativeMode.header2())
            .assertView(
                'header',
                PO.creativeMode.header2(),
                { allowViewportOverflow: true },
            );
    });

    it('должны сворачиваться/разворачиваться все зонты', function() {
        return this.browser
            .preparePage('creative-mode', START_URL)
            .waitForVisible(PO.creativeMode.table.contentRow())
            .click(PO.creativeMode.header.collapseToggler())
            .yaShouldBeVisible(PO.creativeMode.table.contentRow(), undefined, false)
            .assertView(
                'header-rolled',
                PO.creativeMode.header(),
                { allowViewportOverflow: true },
            )
            .click(PO.creativeMode.header.collapseToggler())
            .isVisible(PO.creativeMode.table.contentRow())
            .then(vis => {
                const visible = [].concat(vis);

                visible.map(v => assert.ok(v, 'Какая-то из строк не видима'));
            });
    });

    it('должны сворачиваться все зонты, если развернут хотя бы один', function() {
        return this.browser
            .preparePage('creative-mode', START_URL)
            .waitForVisible(PO.creativeMode.table.contentRow())
            .click(PO.creativeMode.header.collapseToggler())
            .yaShouldBeVisible(PO.creativeMode.table.contentRow(), undefined, false)
            .click(PO.creativeMode.table.headerRow.toggler())
            .yaShouldBeVisible(PO.creativeMode.table.contentRow())
            .click(PO.creativeMode.header.collapseToggler())
            .isVisible(PO.creativeMode.table.contentRow())
            .yaShouldBeVisible(PO.creativeMode.table.contentRow(), undefined, false);
    });

    it('должен сворачиваться/разворачиваться зонт', async function() {
        const rowCount = await this.browser
            .preparePage('creative-mode', START_URL)
            .waitForExist(PO.creativeMode.table.contentRow.id())
            .yaCountElements(PO.creativeMode.table.row());

        return this.browser
            .click(PO.creativeMode.table.headerRow.toggler())
            .yaWaitUntilElCountChanged(
                PO.creativeMode.table.row(),
                1,
            )
            .assertView(
                'folded-umb',
                PO.creativeMode.table(),
                // Ожидаем исчезновения элементов, видимых при ховере
                { screenshotDelay: 1000, allowViewportOverflow: true },
            )
            .click(PO.creativeMode.table.headerRow.toggler())
            .yaWaitUntilElCountChanged(PO.creativeMode.table.row(), rowCount);
    });

    it('у контуров есть кебаб-меню', function() {
        return this.browser
            .preparePage('creative-mode', START_URL)
            .waitForVisible(PO.creativeMode.table.contentRow.id())
            .moveToObject(PO.creativeMode.table.contentRow.id(), 10, 10)
            .assertView(
                'order-with-kebab',
                PO.creativeMode.table.contentRow.id(),
                { dontMoveCursor: true },
            )
            .click(PO.creativeMode.table.contentRow.kebab())
            .waitForVisible(PO.creativeMode.table.contentRow.menu())
            .yaDisableBoxShadow(PO.creativeMode.table.contentRow.menu())
            .assertView(
                'kebab-menu',
                PO.creativeMode.table.contentRow.menu(),
            );
    });

    // eslint-disable-next-line
    it.skip('контур можно перенести в другой зонт', async function() {
        await this.browser
            .preparePage('creative-mode', START_URL)
            .waitForVisible(PO.creativeMode.table.contentRow.id());

        const originalRowCount = await this.browser.yaCountElements(
            PO.creativeMode.secondTable.row(),
        );
        const destinationRowCount = await this.browser.yaCountElements(
            PO.creativeMode.lastTable.row(),
        );

        const movedOutlineID = await this.browser.$(PO.creativeMode.secondTable.lastContentRow())
            .getAttribute('data-id');

        await this.browser
            .preparePage('creative-mode', START_URL)
            .waitForVisible(PO.creativeMode.secondTable.lastContentRow.id())
            .moveToObject(PO.creativeMode.secondTable.lastContentRow.id(), 10, 10)
            .waitForVisible(PO.creativeMode.secondTable.lastContentRow.kebab())
            .click(PO.creativeMode.secondTable.lastContentRow.kebab())
            .waitForVisible(PO.creativeMode.secondTable.lastContentRow.menu.move())
            .click(PO.creativeMode.secondTable.lastContentRow.menu.move())
            .assertView(
                'move-suggest',
                PO.creativeMode.secondTable.lastContentRow.moveSuggest(),
            )
            .click(PO.creativeMode.secondTable.lastContentRow.moveSuggest.input())
            .yaKeyPress('какой')
            .assertView(
                'filtered-move-suggest',
                PO.creativeMode.secondTable.lastContentRow.moveSuggest(),
            )
            .click(PO.creativeMode.secondTable.lastContentRow.moveSuggest.firstResult())
            .yaWaitUntilElCountChanged(PO.creativeMode.secondTable.row(), originalRowCount - 1)
            .yaShouldExist(
                `${PO.creativeMode.secondTable()} .Table-Row[data-id="${movedOutlineID}"]`,
                'Контур должен исчезнуть из старого зонта',
                false,
            )
            .yaWaitUntilElCountChanged(PO.creativeMode.lastTable.row(), destinationRowCount + 1)
            .yaShouldExist(
                `${PO.creativeMode.lastTable()} .Table-Row[data-id="${movedOutlineID}"]`,
                'Контур должен появиться в новом зонте',
            );
    });

    it('контур можно создать и удалить', async function() {
        const rowCount = await this.browser
            .preparePage('creative-mode', START_URL)
            .waitForExist(PO.creativeMode.table.contentRow.id())
            .yaCountElements(PO.creativeMode.table.row());

        // Создание контура
        await this.browser
            .click(PO.creativeMode.table.footer.addButton())
            .yaWaitUntilElCountChanged(
                PO.creativeMode.table.row(),
                rowCount + 1,
            )
            .yaAssertText(PO.creativeMode.table.lastContentRow.title(), 'Untitled OUTLINE')
            .yaAssertText(PO.creativeMode.table.lastContentRow.responsible(), ROBOT_NAME);

        const outlineID = await this.browser.$(PO.creativeMode.table.lastContentRow())
            .getAttribute('data-id');

        // Удаление контура
        await this.browser
            .moveToObject(PO.creativeMode.table.lastContentRow.id(), 10, 10)
            .waitForVisible(PO.creativeMode.table.lastContentRow.kebab())
            .click(PO.creativeMode.table.lastContentRow.kebab())
            .waitForVisible(PO.creativeMode.table.lastContentRow.menu.delete())
            .click(PO.creativeMode.table.lastContentRow.menu.delete())
            .yaWaitUntilElCountChanged(
                PO.creativeMode.table.row(),
                rowCount,
            )
            .yaShouldExist(
                `.GoalsTable-Row[data-id="${outlineID}"]`,
                'Должен удалиться правильный контур',
                false,
            );
    });

    it('зонт можно создать и удалить', async function() {
        const tableCount = await this.browser
            .preparePage('creative-mode', START_URL)
            .waitForExist(PO.creativeMode.table.contentRow.id())
            .yaCountElements(PO.creativeMode.tables());

        await this.browser
            .scroll(PO.creativeMode.addUmbButton());

        // Создание зонта
        await this.browser
            .click(PO.creativeMode.addUmbButton())
            .yaWaitUntilElCountChanged(
                PO.creativeMode.tables(),
                tableCount + 1,
            )
            .yaAssertText(PO.creativeMode.lastTable.headerRow.title(), 'Untitled UMB')
            .yaAssertText(PO.creativeMode.lastTable.headerRow.responsible(), ROBOT_NAME);

        const umbID = await this.browser.$(PO.creativeMode.lastTable())
            .getAttribute('data-id');

        // Удаление зонта
        await this.browser
            .moveToObject(PO.creativeMode.lastTable.headerRow.id(), 10, 10)
            .waitForVisible(PO.creativeMode.lastTable.headerRow.kebab())
            .click(PO.creativeMode.lastTable.headerRow.kebab())
            .waitForVisible(PO.creativeMode.lastTable.headerRow.menu.delete())
            .click(PO.creativeMode.lastTable.headerRow.menu.delete())
            .yaWaitUntilElCountChanged(
                PO.creativeMode.tables(),
                tableCount,
            )
            .yaShouldExist(
                `.GoalsTable-Table[data-id="${umbID}"]`,
                'Должен удалиться правильный зонт',
                false,
            );
    });

    // eslint-disable-next-line
    it.skip('внешний вид полоски vs summary', async function() {
        await this.browser
            .preparePage('creative-mode', START_URL)
            .waitForExist(PO.creativeMode.summary())
            .assertView('vs-summary-strip', PO.creativeMode.summary());
    });

    it('внешний вид полоски umb summary', async function() {
        await this.browser
            .preparePage('creative-mode', START_URL)
            .waitForExist(PO.creativeMode.table.footer.summary())
            .assertView('umb-summary-strip', PO.creativeMode.table.footer.summary());
    });

    describe('кнопка перехода к OKR-дереву', async function() {
        it('должна содержать правильные параметры в ссылке', async function() {
            await this.browser
                .preparePage('creative-mode', START_URL)
                .waitForExist(PO.creativeMode.table.footer.summary())
                .yaCheckHref(PO.creativeMode.header2.goToOKRButton(), {
                    pathname: '/okr',
                    searchParams: {
                        importance: 'all',
                        periods: '2021Q2',
                        statuses: {
                            type: 'array',
                            value: '0,1,2,4,5,173,432,433,450,451',
                        },
                        selectedItem: '57452',
                    },
                });
        });

        // eslint-disable-next-line
        it.skip('должна открывать страницу с выделенным элементом дерева ОКР', async function() {
            await this.browser
                .preparePage('creative-mode', START_URL)
                .waitForExist(PO.creativeMode.table.footer.summary())
                .click(PO.creativeMode.header2.goToOKRButton())
                .waitForExist(PO.newGoalsTree.HighlightedGoalsNode())
                .yaCheckHref(PO.newGoalsTree.HighlightedGoalsNode.TableViewOpener(), {
                    pathname: '/creative-mode/57452',
                    search: /^$/,
                });
        });
    });
});
