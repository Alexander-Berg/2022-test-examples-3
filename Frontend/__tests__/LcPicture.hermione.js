specs({
    feature: 'LcPicture',
}, () => {
    hermione.only.notIn('safari13');
    it('Загружает изображение из аватарницы', function() {
        return this.browser
            .url('/turbo?stub=lcpicture/avatars.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcPicture.imageLoaded(), 'Изображение не загрузилось')
            .waitForExist('source')
            .assertView('plain', PO.lcPicture());
    });

    hermione.only.notIn('safari13');
    it('Загружает изображение без размеров', function() {
        return this.browser
            .url('/turbo?stub=lcpicture/legacy.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcPicture());
    });

    hermione.only.notIn('safari13');
    it('Загружает svg без source', function() {
        return this.browser
            .url('/turbo?stub=lcpicture/svg.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcPicture.imageLoaded(), 'Изображение не загрузилось')
            .waitForExist('source', undefined, true)
            .assertView('plain', PO.lcPicture());
    });
});
