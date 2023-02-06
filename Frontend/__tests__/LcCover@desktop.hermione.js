function test(page) {
    return function() {
        return this.browser
            .url(`/turbo?stub=lccover/${page}.json`)
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView(page, PO.lcCover());
    };
}

specs({
    feature: 'LcCover',
}, () => {
    it('Внешний вид блока с компоновкой текст слева, картинка справа', test('textOnLeftWithImage'));
    it('Внешний вид блока с компоновкой текст слева без картинки', test('textOnLeftWithoutImage'));
    it('Внешний вид блока с компоновкой текст сверху, картинка снизу', test('textOnTopWithImage'));
    it('Внешний вид блока с компоновкой текст сверху без картинки', test('textOnTopWithoutImage'));
    it('Внешний вид блока с компоновкой текст справа, картинка слева', test('textOnRightWithImage'));
});
