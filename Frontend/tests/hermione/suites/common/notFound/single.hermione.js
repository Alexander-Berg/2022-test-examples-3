specs({
    feature: 'Страница не найдена',
}, () => {
    it('Внешний вид', function() {
        const { browser, PO } = this;

        return browser
            .yaOpenPage('/health/not-found')
            .yaStaticHeader()
            .assertView('not-found', PO.NotFoundPage());
    });
});
