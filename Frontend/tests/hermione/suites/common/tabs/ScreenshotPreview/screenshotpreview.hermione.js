describe('Превью', () => {
    describe('Отображение превью', () => {
        beforeEach(function() {
            const { browser, PO } = this;
            return browser
                .yaOpenPage('/')
                .yaScrollDocument(0, 50)
                .click(PO.EnableScreenshot())
                .click(PO.ShowButton())
                .yaWaitForVisible(PO.YndxBug())
                .click(PO.YndxBug())
                .click(PO.Tabs.ScreenshotPreview())
                .yaWaitForVisible(PO.ScreenshotPreview());
        });
        it('На вкладке корректно отображается скриншот', function() {
            const { browser, PO } = this;
            return browser
                .yaAssertView('screenshot', PO.Tabs.ScreenshotPreview());
        });
    });
});
