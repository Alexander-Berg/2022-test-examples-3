specs({
    feature: 'Viewer',
}, () => {
    hermione.only.notIn('safari13');
    it('Восстанавливает позицию скролла', function() {
        const EXPECTED_SCROLL_TOP = 850;
        return this.browser
            .url('/turbo?stub=viewer/scroll-restoration.json')
            .yaWaitForVisible(PO.pageJsInited(), 'Не загрузилась страница')
            .yaIndexify(PO.image())
            .yaScrollPage(EXPECTED_SCROLL_TOP)
            .click(PO.thirdImage())
            .yaShouldBeVisible(PO.viewer(), 'Viewer не открылся')
            .click(PO.viewerHeader.close())
            .yaShouldNotBeVisible(PO.viewer(), 'Viewer не закрылся')
            .execute(() => document.documentElement && document.documentElement.scrollTop)
            .then(({ value: actualScrollTop }) => {
                assert(EXPECTED_SCROLL_TOP === actualScrollTop, 'Позиция скролла не восстановлена');
            });
    });
});
