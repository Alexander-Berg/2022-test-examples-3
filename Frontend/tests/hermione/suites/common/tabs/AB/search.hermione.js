describe('Эксперименты', function() {
    describe('Поиск', function() {
        beforeEach(function() {
            const { browser, PO } = this;

            return browser
                .yaOpenPage('/')
                .yaWaitForVisible(PO.YndxBug())
                .click(PO.YndxBug())
                .click(PO.Tabs.AB())
                .yaWaitForVisible(PO.AB.StuckPage.Input());
        });

        it('Поле поиска с введенным "чистый яндекс"', function() {
            const { browser, PO } = this;

            return browser
                .setValue(PO.AB.StuckPage.Input(), 'чистый яндекс')
                .yaCheckValue(PO.AB.StuckPage.Input(), 'чистый яндекс')
                .yaAssertView('ABInput', PO.AB.StuckPage.Input());
        });
    });
});
