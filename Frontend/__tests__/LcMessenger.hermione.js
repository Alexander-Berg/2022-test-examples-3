specs({
    feature: 'LcMessenger',
}, () => {
    it('Внешний вид виджета', function() {
        return this.browser
            .url('/turbo?stub=lcmessenger/default.json')
            .yaWaitForVisible(PO.lcMessenger(), 'Виджет не загрузился')
            .assertView('plain', PO.lcMessenger());
    });

    it('Виджет на странице', function() {
        return this.browser
            .url('/turbo?stub=lcmessenger/page.json')
            .yaWaitForVisible(PO.lcMessenger(), 'Виджет не загрузился')
            .yaScrollPage(0)
            .assertView('plain', PO.page());
    });
});
