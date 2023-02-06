specs({
    feature: 'LcImageLpc',
}, () => {
    hermione.only.notIn('safari13');
    it('Обычное изображение', function() {
        return this.browser
            .url('/turbo?stub=lcimagelpc/default.json')
            .yaWaitForVisible(PO.lcImageLpc(), 'Изображение не загрузилось')
            .assertView('plain', PO.lcImageLpc());
    });

    hermione.only.notIn('safari13');
    it('Изображение с кастомной шириной', function() {
        return this.browser
            .url('/turbo?stub=lcimagelpc/custom-width.json')
            .yaWaitForVisible(PO.lcImageLpc(), 'Изображение не загрузилось')
            .assertView('plain', PO.lcImageLpc());
    });
});
