specs({
    feature: 'Спорт: плашка со счетом',
}, () => {
    it('корректно отображается', function() {
        return this.browser
            .url('/turbo?stub=sportscoreplate/default.json')
            .windowHandleSize({ width: 1000, height: 600 })
            .assertView('plain', PO.blocks.page())
            .windowHandleSize({ width: 1600, height: 600 })
            .assertView('plain-1600', PO.blocks.page());
    });
});
