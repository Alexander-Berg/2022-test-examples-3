specs(
    {
        feature: 'EmcSection',
    },
    () => {
        hermione.only.notIn('safari13');
        it('Обычный внешний вид секции', function() {
            return this.browser
                .url('/turbo?stub=emcsection/default.json')
                .yaWaitForVisible(PO.emcPage(), 'Страница не загрузилась')
                .assertView('emcsection', PO.emcPage());
        });

        hermione.only.notIn('safari13');
        it('Внешний вид секций с фоновым цветом и различными настройками отступов', function() {
            return this.browser
                .url('/turbo?stub=emcsection/offsets.json')
                .yaWaitForVisible(PO.emcPage(), 'Страница не загрузилась')
                .assertView('emcsection', PO.emcPage());
        });

        hermione.only.notIn('safari13');
        it('Внешний вид секций с различными настройками размера фоновой картинки', function() {
            return this.browser
                .url('/turbo?stub=emcsection/sizes.json')
                .yaWaitForVisible(PO.emcPage(), 'Страница не загрузилась')
                .assertView('emcsection', PO.emcPage());
        });

        hermione.only.notIn('safari13');
        it('Внешний вид секций с различными настройками позиционирования фоновой картинки', function() {
            return this.browser
                .url('/turbo?stub=emcsection/positions.json')
                .yaWaitForVisible(PO.emcPage(), 'Страница не загрузилась')
                .assertView('emcsection', PO.emcPage());
        });

        hermione.only.notIn('safari13');
        it('Внешний вид секций с различными настройками границ', function() {
            return this.browser
                .url('/turbo?stub=emcsection/border.json')
                .yaWaitForVisible(PO.emcPage(), 'Страница не загрузилась')
                .assertView('emcsection', PO.emcPage());
        });

        hermione.only.notIn('safari13');
        it('Внешний вид секций с различными настройками скругления', function() {
            return this.browser
                .url('/turbo?stub=emcsection/border-radius.json')
                .yaWaitForVisible(PO.emcPage(), 'Страница не загрузилась')
                .assertView('emcsection', PO.emcPage());
        });
    }
);
