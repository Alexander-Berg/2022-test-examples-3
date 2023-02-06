specs({
    feature: 'LcYandexForm',
}, () => {
    it('Внешний вид формы', function() {
        return this.browser
            .setViewportSize(({
                width: 1100,
                height: 800,
            }))
            .url('/turbo?stub=lcyandexform/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcPage(), {
                screenshotDelay: 2000,
                allowViewportOverflow: true,
            });
    });

    it('Внешний вид формы, когда прокидываем флаг device=phone', function() {
        return this.browser
            .setViewportSize(({
                width: 1100,
                height: 800,
            }))
            .url('/turbo?stub=lcyandexform/default.json&device=phone')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('mobile', PO.lcPage(), {
                screenshotDelay: 2000,
                allowViewportOverflow: true,
            });
    });
});
