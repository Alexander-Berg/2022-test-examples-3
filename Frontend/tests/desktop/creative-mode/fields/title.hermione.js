const assert = require('chai').assert;

const PO = require('../../../../page-objects');
const START_URL = '/creative-mode/56690';
const LINK_URL = {
    pathname: '/okr',
    searchParams: {
        importance: 'all',
        periods: {
            type: 'array',
            value: '2021Q1,2021Q2',
        },
        selectedItem: '56691',
        goal: '56691',
        statuses: {
            type: 'array',
            value: '0,1,2,4,5,173,432,433,450,451',
        },
    },
};
const FULLSCREEN_HASH = '#56691';

describe('Поля режима креатива', function() {
    describe('title', function() {
        beforeEach(function() {
            return this.browser
                .setViewportSize({ width: 1700, height: 2000 })
                .loginToGoals();
        });

        it('заголовок зонтика редактируется', function() {
            return this.browser
                .preparePage('creative-mode-title', START_URL)
                .waitForVisible(PO.creativeMode.table.headerRow.title())
                .assertView('umb-title-plain', PO.creativeMode.table.headerRow.title())
                .doubleClick(PO.creativeMode.table.headerRow.title())
                .assertView('umb-title-editable', PO.creativeMode.table.headerRow.title())
                .yaKeyPress('edited umb')
                .assertView('umb-title-changed', PO.creativeMode.table.headerRow.title())
                .yaKeyPress('ENTER')
                .assertView('umb-title-saved', PO.creativeMode.table.headerRow.title());
        });

        it('заголовок зонтика кнопка внешняя ссылка', function() {
            return this.browser
                .preparePage('creative-mode-title', START_URL)
                .waitForVisible(PO.creativeMode.table.headerRow.title())
                .moveToObject(PO.creativeMode.table.headerRow.title(), 10, 10)
                .waitForVisible(PO.creativeMode.table.headerRow.title.actionButtons())
                .waitForVisible(PO.creativeMode.table.headerRow.title.actionButtons.goalLink())
                .assertView(
                    'creative-mode-title-hovered',
                    PO.creativeMode.table.headerRow.title(),
                    { dontMoveCursor: true },
                )
                .moveToObject(PO.creativeMode.table.headerRow.title.actionButtons.goalLink(), 10, 10)
                .assertView(
                    'creative-mode-title-goal-link-hovered',
                    PO.creativeMode.table.headerRow.title(),
                    { dontMoveCursor: true },
                )
                .yaCheckHref(
                    PO.creativeMode.table.headerRow.title.actionButtons.goalLink(),
                    LINK_URL,
                );
        });

        it('заголовок зонтика кнопка полноэкранный режим', function() {
            return this.browser
                .preparePage('creative-mode-title', START_URL)
                .waitForVisible(PO.creativeMode.table.headerRow.title())
                .moveToObject(PO.creativeMode.table.headerRow.title(), 10, 10)
                .waitForVisible(PO.creativeMode.table.headerRow.title.actionButtons())
                .waitForVisible(PO.creativeMode.table.headerRow.title.actionButtons.fullScreen())
                .moveToObject(PO.creativeMode.table.headerRow.title.actionButtons.fullScreen(), 10, 10)
                .assertView(
                    'creative-mode-title-fullscreen-hovered',
                    PO.creativeMode.table.headerRow.title(),
                    { dontMoveCursor: true },
                )
                .click(PO.creativeMode.table.headerRow.title.actionButtons.fullScreen())
                .yaGetParsedUrl()
                .then(val => assert.equal(val.hash, FULLSCREEN_HASH))
                // если будет больше одной таблицы (видимой или нет) - проверка упадет
                .yaShouldBeVisible(PO.creativeMode.table());
        });

        it('редактирование title сбрасывается по escape', function() {
            return this.browser
                .preparePage('creative-mode-title', START_URL)
                .waitForVisible(PO.creativeMode.table.headerRow.title())
                .assertView('umb-title-plain', PO.creativeMode.table.headerRow.title())
                .doubleClick(PO.creativeMode.table.headerRow.title())
                .assertView('umb-title-editable', PO.creativeMode.table.headerRow.title())
                .yaKeyPress('edited umb')
                .assertView('umb-title-changed', PO.creativeMode.table.headerRow.title())
                .yaKeyPress('ESC')
                .assertView('umb-title-unchanged', PO.creativeMode.table.headerRow.title());
        });

        it('заголовок контура редактируется', function() {
            return this.browser
                .preparePage('creative-mode-title', START_URL)
                .waitForVisible(PO.creativeMode.table.contentRow.title())
                .assertView('outline-title-plain', PO.creativeMode.table.contentRow.title())
                .doubleClick(PO.creativeMode.table.contentRow.title())
                .assertView('outline-title-editable', PO.creativeMode.table.contentRow.title())
                .yaKeyPress('edited outline')
                .assertView('outline-title-changed', PO.creativeMode.table.contentRow.title())
                .yaKeyPress('ENTER')
                .assertView('outline-title-saved', PO.creativeMode.table.contentRow.title());
        });
    });
});
