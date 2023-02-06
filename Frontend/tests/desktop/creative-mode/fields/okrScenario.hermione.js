/* eslint-disable mocha/no-skipped-tests */
const PO = require('../../../../page-objects');
const START_URL = '/creative-mode/56690';

describe('Поля режима креатива', function() {
    beforeEach(function() {
        return this.browser
            .setViewportSize({ width: 1700, height: 2000 })
            .loginToGoals();
    });

    it.skip('сценарий зонтика редактируется', function() {
        return this.browser
            .preparePage('creative-mode-okrScenario', START_URL)
            .waitForVisible(PO.creativeMode.table.headerRow.okrScenario())
            .assertView('umb-scenario-plain', PO.creativeMode.table.headerRow.okrScenario())
            .doubleClick(PO.creativeMode.table.headerRow.okrScenario())
            .assertView('umb-scenario-editable', PO.creativeMode.table.headerRow.okrScenario())
            .yaKeyPress(['BACKSPACE', 'BACKSPACE', 'BACKSPACE', '90'])
            .assertView('umb-scenario-changed', PO.creativeMode.table.headerRow.okrScenario())
            .yaKeyPress('ENTER')
            .assertView('umb-scenario-saved', PO.creativeMode.table.headerRow.okrScenario());
    });

    it.skip('редактирование сценария сбрасывается по escape', function() {
        return this.browser
            .preparePage('creative-mode-okrScenario', START_URL)
            .waitForVisible(PO.creativeMode.table.headerRow.okrScenario())
            .assertView('umb-scenario-plain', PO.creativeMode.table.headerRow.okrScenario())
            .doubleClick(PO.creativeMode.table.headerRow.okrScenario())
            .assertView('umb-scenario-editable', PO.creativeMode.table.headerRow.okrScenario())
            .yaKeyPress(['BACKSPACE', 'BACKSPACE', 'BACKSPACE', '90'])
            .assertView('umb-scenario-changed', PO.creativeMode.table.headerRow.okrScenario())
            .yaKeyPress('ESC')
            .assertView('umb-scenario-unchanged', PO.creativeMode.table.headerRow.okrScenario());
    });

    it.skip('сценарий контура редактируется', function() {
        return this.browser
            .preparePage('creative-mode-okrScenario', START_URL)
            .waitForVisible(PO.creativeMode.table.contentRow.okrScenario())
            .assertView('outline-scenario-plain', PO.creativeMode.table.contentRow.okrScenario())
            .doubleClick(PO.creativeMode.table.contentRow.okrScenario())
            .assertView('outline-scenario-editable', PO.creativeMode.table.contentRow.okrScenario())
            .yaKeyPress(['BACKSPACE', 'BACKSPACE', 'BACKSPACE', '90'])
            .assertView('outline-scenario-changed', PO.creativeMode.table.contentRow.okrScenario())
            .yaKeyPress('ENTER')
            .assertView('outline-scenario-saved', PO.creativeMode.table.contentRow.okrScenario());
    });
});
