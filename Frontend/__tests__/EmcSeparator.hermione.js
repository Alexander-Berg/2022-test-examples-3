specs(
    {
        feature: 'EmcSeparator',
    },
    () => {
        hermione.only.notIn('safari13');
        it('Обычный разделитель', function() {
            return this.browser
                .url('/turbo?stub=emcseparator/default.json')
                .yaWaitForVisible(PO.emcSeparator(), 'Разделитель не появился')
                .assertView('emcseparator', PO.emcSeparator());
        });

        hermione.only.notIn('safari13');
        it('Разделитель с маленькими отступами', function() {
            return this.browser
                .url('/turbo?stub=emcseparator/default-offsets.json')
                .yaWaitForVisible(PO.emcSeparator(), 'Разделитель не появился')
                .assertView('emcseparator', PO.emcSeparator());
        });

        hermione.only.notIn('safari13');
        it('Разделитель с фоновым цветом', function() {
            return this.browser
                .url('/turbo?stub=emcseparator/default-colors.json')
                .yaWaitForVisible(PO.emcSeparator(), 'Разделитель не появился')
                .assertView('emcseparator', PO.emcSeparator());
        });

        hermione.only.notIn('safari13');
        it('Тонкий точечный разделитель', function() {
            return this.browser
                .url('/turbo?stub=emcseparator/thin-dotted.json')
                .yaWaitForVisible(PO.emcSeparator(), 'Разделитель не появился')
                .assertView('emcseparator', PO.emcSeparator());
        });

        hermione.only.notIn('safari13');
        it('Толстый сплошной разделитель', function() {
            return this.browser
                .url('/turbo?stub=emcseparator/thick.json')
                .yaWaitForVisible(PO.emcSeparator(), 'Разделитель не появился')
                .assertView('emcseparator', PO.emcSeparator());
        });

        hermione.only.notIn('safari13');
        it('Толстый разделитель короткими линиями', function() {
            return this.browser
                .url('/turbo?stub=emcseparator/thick-dashed.json')
                .yaWaitForVisible(PO.emcSeparator(), 'Разделитель не появился')
                .assertView('emcseparator', PO.emcSeparator());
        });

        hermione.only.notIn('safari13');
        it('Обычный разделитель (в колонке)', function() {
            return this.browser
                .url('/turbo?stub=emcseparator/default-columns.json')
                .yaWaitForVisible(PO.emcSeparator(), 'Разделитель не появился')
                .assertView('emcseparator', PO.emcSeparator());
        });

        hermione.only.notIn('safari13');
        it('Разделитель с фоновым цветом (в колонке)', function() {
            return this.browser
                .url('/turbo?stub=emcseparator/default-colors-columns.json')
                .yaWaitForVisible(PO.emcSeparator(), 'Разделитель не появился')
                .assertView('emcseparator', PO.emcSeparator());
        });
    }
);
