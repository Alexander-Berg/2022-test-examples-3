specs({
    feature: 'PriceRange',
}, () => {
    hermione.only.notIn('safari13');
    function visit(url, browser) {
        return browser
            .url(url)
            .yaWaitForVisible(PO.priceRange(), 'Блок выбора ценового диапазона не появился на странице');
    }

    const firstInput = PO.priceRange.firstInput();

    function assertInput(expected, browser) {
        return browser
            .getValue(firstInput)
            .then(value => assert.equal(value, expected));
    }

    hermione.only.notIn('safari13');
    it('Базовый вид', function() {
        return visit('/turbo?stub=pricerange/empty.json&l10n=en', this.browser)
            .assertView('plain', PO.priceRange());
    });

    hermione.only.in('chrome-phone', 'setOrientation() используем только в chrome-phone');
    hermione.only.notIn('safari13');
    it('Базовый вид в горизонтальной ориентации', function() {
        return this.browser
            .setOrientation('landscape')
            .then(() => visit('/turbo?stub=pricerange/empty.json', this.browser))
            .assertView('plain', PO.priceRange());
    });

    hermione.only.notIn('safari13');
    it('Базовый вид с заполненными значениями', function() {
        return visit('/turbo?stub=pricerange/with-values.json', this.browser)
            .assertView('plain', PO.priceRange());
    });

    hermione.only.notIn('safari13');
    it('Нельзя ввести больше 15 символов и не натуральные числа', function() {
        return visit('/turbo?stub=pricerange/empty.json', this.browser)
            .setValue(firstInput, '010a3g+45-6789.012,34567')
            .then(() => assertInput('103456789012345', this.browser));
    });

    hermione.only.notIn('safari13');
    it('Правильное значение при удалении символов', function() {
        return visit('/turbo?stub=pricerange/empty.json', this.browser)
            .setValue(firstInput, '1004')
            .then(() => assertInput('1004', this.browser))
            .click(firstInput)
            .keys(['ArrowLeft', 'ArrowLeft', 'Backspace'])
            .then(() => assertInput('104', this.browser))
            .keys('Backspace')
            .then(() => assertInput('4', this.browser));
    });
});
