// TODO: Добавить кейс с мультивыбором.
describe('Select', () => {
    hermione.skip.in(/./);
    describe('classic-design', () => {
        ['s', 'm'].forEach((size) => {
            it(`size-${size}`, function() {
                return (
                    this.browser
                        .url('Select/hermione/hermione.html')
                        .assertView('plain', ['.Classic .Hermione-Row_type_sizes'])
                        .click(`.Classic .Hermione-Row_type_sizes .Hermione-Item_size_${size} .Select2-Button`)
                        // Ставим паузу, чтобы все компоненты успели раскрыться.
                        .pause(200)
                        .assertView('opened', [
                            `.Classic .Hermione-Row_type_sizes .Hermione-Item_size_${size} .Select2-Button`,
                            '.Popup2_visible',
                        ])
                        .click('.Popup2_visible .Menu-Item')
                        .assertView('selected', [
                            `.Classic .Hermione-Row_type_sizes .Hermione-Item_size_${size} .Select2-Button`,
                        ])
                        .click(`.Classic .Hermione-Row_type_sizes .Hermione-Item_size_${size} .Complex .Select2-Button`)
                        // Ставим паузу, чтобы все компоненты успели раскрыться.
                        .pause(200)
                        .assertView('complex-opened', [
                            `.Classic .Hermione-Row_type_sizes .Hermione-Item_size_${size} .Select2-Button`,
                            '.Popup2_visible',
                        ])
                );
            });
        });

        it('fixed-height', function() {
            return (
                this.browser
                    .url('Select/hermione/hermione.html')
                    .click('.Classic .Hermione-Row_type_fixed-height .Select2-Button')
                    // Ставим паузу, чтобы все компоненты успели раскрыться.
                    .pause(200)
                    .assertView('opened', [
                        '.Classic .Hermione-Row_type_fixed-height .Select2-Button',
                        '.Popup2_visible',
                    ])
            );
        });

        hermione.skip.in(/./);
        it('width-max', function() {
            return (
                this.browser
                    .url('Select/hermione/hermione.html')
                    .click('.Classic .Hermione-Row_type_width-max .Select2-Button')
                    // Ставим паузу, чтобы все компоненты успели раскрыться.
                    .pause(200)
                    .assertView('opened', ['.Classic .Hermione-Row_type_width-max .Select2-Button', '.Popup2_visible'])
            );
        });

        it('width-fixed', function() {
            return (
                this.browser
                    .url('Select/hermione/hermione.html')
                    .click('.Classic .Hermione-Row_type_width-fixed .Select2-Button')
                    // Ставим паузу, чтобы все компоненты успели раскрыться.
                    .pause(200)
                    .assertView('opened', [
                        '.Classic .Hermione-Row_type_width-fixed .Select2-Button',
                        '.Popup2_visible',
                    ])
            );
        });

        hermione.skip.in(/./);
        it('long-content', function() {
            return (
                this.browser
                    .url('Select/hermione/hermione.html')
                    .click('.Classic .Hermione-Row_type_long-content .Select2-Button')
                    // Ставим паузу, чтобы все компоненты успели раскрыться.
                    .pause(200)
                    .assertView('opened', [
                        '.Classic .Hermione-Row_type_long-content .Select2-Button',
                        '.Popup2_visible',
                    ])
            );
        });
    });

    hermione.skip.in(/./);
    describe('new-design', () => {
        // TODO: Для остальных тем попап рендерится не в скоупе, из-за этого имеет дефолтный цвет.
        [
            'yandex-default',
            /*, 'yandex-inverse', 'yandex-brand'*/
        ].forEach((color) => {
            describe(color, () => {
                ['s', 'm'].forEach((size) => {
                    it(`size-${size}`, function() {
                        return (
                            this.browser
                                .url('Select/hermione/hermione.html')
                                .assertView('plain', [`.New .Hermione-Row_color_${color}`])
                                .click(`.New .Hermione-Row_color_${color} .Hermione-Item_size_${size} .Select2-Button`)
                                // Ставим паузу, чтобы все компоненты успели раскрыться.
                                .pause(200)
                                .assertView('opened', [
                                    `.New .Hermione-Row_color_${color} .Hermione-Item_size_${size} .Select2-Button`,
                                    '.Popup2_visible',
                                ])
                                .click('.Popup2_visible .Menu-Item')
                                .assertView('selected', [
                                    `.New .Hermione-Row_color_${color} .Hermione-Item_size_${size} .Select2-Button`,
                                ])
                                .click(
                                    `.New .Hermione-Row_color_${color} .Hermione-Item_size_${size} .Complex .Select2-Button`,
                                )
                                // Ставим паузу, чтобы все компоненты успели раскрыться.
                                .pause(200)
                                .assertView('complex-opened', [
                                    `.New .Hermione-Row_color_${color} .Hermione-Item_size_${size} .Select2-Button`,
                                    '.Popup2_visible',
                                ])
                        );
                    });
                });
            });
        });
    });

    const elements = {
        trigger: '.Button2',
        popup: '.Popup2',
        container: '[data-testid=container]',
    };

    it('should render select and prevent overflow', function() {
        return this.browser
            .url('SelectScenarios/hermione/hermione.html?scenario=long')
            .setViewportSize({ width: 800, height: 600 })
            .click(elements.trigger)
            .waitForVisible(elements.popup)
            .assertView('opened', [elements.container]);
    });
});
