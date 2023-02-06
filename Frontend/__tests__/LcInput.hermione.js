const modes = ['dc', 'lpc'];
const sizes = ['xs', 's', 'm', 'l', 'xl'];

specs({
    feature: 'LcInput',
}, () => {
    // Прогоняем одинаковые тесты для двух режимов
    modes.forEach(mode => {
        describe(`В режиме ${mode}`, () => {
            hermione.only.notIn('safari13');
            it('Обычное поле', function() {
                return this.browser
                    .url(`/turbo?stub=lcinput/${mode}-default.json`)
                    .yaWaitForVisible(PO.lcInput(), 'Текстовое поле не появилось')
                    .assertView(`${mode}-default`, PO.lcInput());
            });

            hermione.only.notIn('safari13');
            it('Поле с надписью', function() {
                return this.browser
                    .url(`/turbo?stub=lcinput/${mode}-label.json`)
                    .yaWaitForVisible(PO.lcInput(), 'Текстовое поле не появилось')
                    .assertView(`${mode}-label`, PO.lcInput());
            });

            hermione.only.notIn('safari13');
            it('Невалидное поле', function() {
                return this.browser
                    .url(`/turbo?stub=lcinput/${mode}-invalid.json`)
                    .yaWaitForVisible(PO.lcInput(), 'Текстовое поле не появилось')
                    .assertView(`${mode}-invalid`, PO.lcInput());
            });

            hermione.only.notIn('safari13');
            it('Заполненное поле', function() {
                return this.browser
                    .url(`/turbo?stub=lcinput/${mode}-filled.json`)
                    .yaWaitForVisible(PO.lcInput(), 'Текстовое поле не появилось')
                    .assertView(`${mode}-filled`, PO.lcInput());
            });

            hermione.only.notIn('safari13');
            it('Недоступное поле', function() {
                return this.browser
                    .url(`/turbo?stub=lcinput/${mode}-disabled.json`)
                    .yaWaitForVisible(PO.lcInput(), 'Текстовое поле не появилось')
                    .assertView(`${mode}-disabled`, PO.lcInput());
            });

            // Проверяем все размеры
            sizes.forEach(size => {
                hermione.only.notIn('safari13');
                it(`Размер ${size}`, function() {
                    return this.browser.url(`/turbo?stub=lcinput/${mode}-size-${size}.json`)
                        .yaWaitForVisible(PO.lcInput(), 'Текстовое поле не появилось')
                        .assertView(`${mode}-size-${size}`, PO.lcInput());
                });
            });

            hermione.only.notIn('safari13');
            it('Поле электронной почты в фокусе', function() {
                return this.browser
                    .url(`/turbo?stub=lcinput/${mode}-email.json`)
                    .yaWaitForVisible(PO.lcInput(), 'Текстовое поле не появилось')
                    .click(PO.lcInput.email.input())
                    .keys('turbo lpc...'.split(''))
                    .assertView(`${mode}-email`, PO.lcInput());
            });
        });
    });
});
