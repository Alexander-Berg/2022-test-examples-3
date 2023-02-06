specs({
    feature: 'LcChat',
}, () => {
    it('Внешний вид без id чата', function() {
        return this.browser
            .url('/turbo?stub=lcchat/no-chat-id.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcChat());
    });
});
