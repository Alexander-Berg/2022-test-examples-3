hermione.only.in(['chrome-desktop', 'firefox']);
specs({
    feature: 'LcGridBlockChat',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид без id чата', function() {
        return this.browser
            .url('/turbo?stub=lcgridblockchat/no-chat-id.json')
            .yaWaitForVisible(PO.lcGridPattern(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGrid());
    });
});
