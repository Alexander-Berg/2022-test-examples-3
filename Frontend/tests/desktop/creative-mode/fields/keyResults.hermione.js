const PO = require('../../../../page-objects');
const START_URL = '/creative-mode/56690';

describe('Поля режима креатива', function() {
    describe('keyResults', function() {
        beforeEach(function() {
            return this.browser
                .setViewportSize({ width: 1700, height: 2000 })
                .loginToGoals();
        });

        it('keyResults зонтика редактируется', function() {
            return this.browser
                .preparePage('creative-mode-keyResults', START_URL)
                .waitForVisible(PO.creativeMode.table.headerRow.keyResults())
                .assertView('umb-keyResults-plain', PO.creativeMode.table.headerRow.keyResults())
                .doubleClick(PO.creativeMode.table.headerRow.keyResults())
                .assertView('umb-keyResults-editable', PO.creativeMode.table.headerRow.keyResults())
                .yaKeyPress(['56', 'ENTER', 'new keyResults'])
                .assertView('umb-keyResults-changed', PO.creativeMode.table.headerRow.keyResults())
                .yaKeyPress('Shift+ENTER')
                .assertView('umb-keyResults-saved', PO.creativeMode.table.headerRow.keyResults());
        });

        it('редактирование keyResults сбрасывается по escape', function() {
            return this.browser
                .preparePage('creative-mode-keyResults', START_URL)
                .waitForVisible(PO.creativeMode.table.headerRow.keyResults())
                .assertView('umb-keyResults-plain', PO.creativeMode.table.headerRow.keyResults())
                .doubleClick(PO.creativeMode.table.headerRow.keyResults())
                .assertView('umb-keyResults-editable', PO.creativeMode.table.headerRow.keyResults())
                .yaKeyPress(['56', 'ENTER', 'new keyResults'])
                .assertView('umb-keyResults-changed', PO.creativeMode.table.headerRow.keyResults())
                .yaKeyPress('ESC')
                .waitForVisible(PO.creativeMode.table.headerRow.keyResults())
                .assertView('umb-keyResults-unchanged', PO.creativeMode.table.headerRow.keyResults());
        });

        it('keyResults контура редактируется', function() {
            return this.browser
                .preparePage('creative-mode-keyResults', START_URL)
                .waitForVisible(PO.creativeMode.table.contentRow.keyResults())
                .assertView('outline-keyResults-plain', PO.creativeMode.table.contentRow.keyResults())
                .doubleClick(PO.creativeMode.table.contentRow.keyResults())
                .assertView('outline-keyResults-editable', PO.creativeMode.table.contentRow.keyResults())
                .yaKeyPress(['72', 'ENTER', 'new objective'])
                .assertView('outline-keyResults-changed', PO.creativeMode.table.contentRow.keyResults())
                .yaKeyPress('Shift+ENTER')
                .assertView('outline-keyResults-saved', PO.creativeMode.table.contentRow.keyResults());
        });
    });
});
