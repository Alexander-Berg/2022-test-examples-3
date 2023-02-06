specs({
    feature: 'LcVideoBlock',
}, () => {
    hermione.only.notIn('safari13');
    it('Обычный блок видео', function() {
        return this.browser
            .url('/turbo?stub=lcvideoblock/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcVideoBlock(), {
                ignoreElements: ['.lc-video-block__video-object'],
            });
    });
});
