specs({
    feature: 'LcBackground',
}, () => {
    function checkBG(browser, expName, element = PO.lcBackground()) {
        return browser
            .url(`/turbo?stub=lcbackground/${expName}.json`)
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', element)
            .yaCheckClientErrors();
    }

    hermione.only.notIn('safari13');
    it('Внешний вид блока с цветным фоном', function() {
        return checkBG(this.browser, 'default');
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока с фоном картинкой', function() {
        return checkBG(this.browser, 'with-image');
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блоков с фоном картинкой с разными настройками размеров', function() {
        return checkBG(this.browser, 'sizes', PO.hermioneContainer());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блоков с фоном картинкой с разными настройками позиционирования', function() {
        return checkBG(this.browser, 'positions', PO.hermioneContainer());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блоков с фоном картинкой с кастомными настройками позиционирования', function() {
        return checkBG(this.browser, 'custom-positions', PO.hermioneContainer());
    });
});
