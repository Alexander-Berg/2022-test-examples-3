specs({
    feature: 'EmcImage',
}, () => {
    hermione.only.notIn('safari13');
    it('Обычное изображение', function() {
        return this.browser
            .url('/turbo?stub=emcimage/default.json')
            .yaWaitForVisible(PO.emcImage(), 'Изображение не появилось')
            .assertView('emcimage', PO.emcImage());
    });

    hermione.only.notIn('safari13');
    it('Изображение с фоном, отступами и фиксированной шириной', function() {
        return this.browser
            .url('/turbo?stub=emcimage/background-offsets-width.json')
            .yaWaitForVisible(PO.emcImage(), 'Изображение не появилось')
            .assertView('emcimage', PO.emcImage());
    });

    hermione.only.notIn('safari13');
    it('Обычное изображение (в колонке)', function() {
        return this.browser
            .url('/turbo?stub=emcimage/default-columns.json')
            .yaWaitForVisible(PO.emcImage(), 'Изображение не появилось')
            .assertView('emcimage', PO.emcImage());
    });

    hermione.only.notIn('safari13');
    it('Изображение с фоном, отступами и фиксированной шириной (в колонке)', function() {
        return this.browser
            .url('/turbo?stub=emcimage/background-offsets-width-columns.json')
            .yaWaitForVisible(PO.emcImage(), 'Изображение не появилось')
            .assertView('emcimage', PO.emcImage());
    });

    hermione.only.notIn('safari13');
    it('Изображение, выравненное по левому краю', function() {
        return this.browser
            .url('/turbo?stub=emcimage/left-aligned.json')
            .yaWaitForVisible(PO.emcImage(), 'Изображение не появилось')
            .assertView('emcimage', PO.emcImage());
    });

    hermione.only.notIn('safari13');
    it('Изображение, выравненное по левому краю (в колонке)', function() {
        return this.browser
            .url('/turbo?stub=emcimage/left-aligned-columns.json')
            .yaWaitForVisible(PO.emcImage(), 'Изображение не появилось')
            .assertView('emcimage', PO.emcImage());
    });

    hermione.only.notIn('safari13');
    it('Изображение, выравненное по правому краю', function() {
        return this.browser
            .url('/turbo?stub=emcimage/right-aligned.json')
            .yaWaitForVisible(PO.emcImage(), 'Изображение не появилось')
            .assertView('emcimage', PO.emcImage());
    });

    hermione.only.notIn('safari13');
    it('Изображение, выравненное по правому краю (в колонке)', function() {
        return this.browser
            .url('/turbo?stub=emcimage/right-aligned-columns.json')
            .yaWaitForVisible(PO.emcImage(), 'Изображение не появилось')
            .assertView('emcimage', PO.emcImage());
    });
});
