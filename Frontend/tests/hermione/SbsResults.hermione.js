// Результаты экспериментов уехали из Mongo в Argentum
// TODO научиться кешировать ответы Samadhi и разскипать SBSDEV-5565
describe.skip('SbsResults', function() {
    afterEach(function() {
        return this.browser.yaCheckClientErrors();
    });

    describe('layouts', function() {
        it('SbsResultsPairedTable, SbsSystemsSummary:', function() {
            return this.browser
                .openSbs('/experiment/10961?argentum=0')
                .waitForVisible('.SbsSystemsSummary', 6000)
                .assertView('SbsSystemsSummary', '.SbsSystemsSummary', { ignoreElements: ['.SbsSystemsSummary-Container img'] })
                .click('.SbsResults-SystemsSummarySelector .radio-button__radio_side_left')
                .waitForVisible('.SbsResultsPairedTable_type_win-rate', 1000)
                .scroll('.SbsResultsStats')
                .assertView('SbsResultsPairedTable_type_win-rate', '.SbsResultsPairedTable_type_win-rate');
        });
    });
});
