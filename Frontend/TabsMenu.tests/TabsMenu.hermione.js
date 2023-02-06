describe('TabsMenu', () => {
    describe('classic-design', () => {
        ['s', 'm'].forEach((size) => {
            it(`size-${size}`, function() {
                return this.browser
                    .url('TabsMenu/hermione/hermione.html')
                    // Сдвигаем курсор, т.к. в ie11 курсор первоначально находится на компоненте,
                    // из-за этого возникает hovered эффект.
                    .moveToObject('body', 250, 250)
                    .assertView('plain', [`.Classic .Hermione-Item_size_${size}`]);
            });
        });
    });

    describe('new-design', () => {
        ['yandex-default', 'yandex-inverse', 'yandex-brand'].forEach((color) => {
            describe(color, () => {
                ['s', 'm'].forEach((size) => {
                    it(`size-${size}`, function() {
                        return this.browser
                            .url('TabsMenu/hermione/hermione.html')
                            .assertView('plain', [`.New .Hermione-Item_size_${size}.Hermione-Item_color_${color}`]);
                    });
                });
            });
        });
    });

    describe('adaptive', () => {
        describe('classic-design', () => {
            ['s', 'm'].forEach((size) => {
                hermione.skip.in(['firefox', 'linux-firefox'], 'setViewportSize is not working in firefox', { silent: true });
                it(`size-${size}`, function() {
                    const selector = [`.Adaptive .Hermione-Item_size_${size}`];

                    return this.browser
                        .url('TabsMenu/hermione/hermione.html')
                        .setViewportSize({ width: 800, height: 1600 })
                        .assertView('all', selector)
                        .setViewportSize({ width: 250, height: 1600 })
                        .assertView('small', selector);
                });
            });
        });
    });

    describe('adaptive-with-adding-and-deleting', () => {
        describe('classic-design', () => {
            ['m'].forEach((size) => {
                hermione.skip.in(['firefox', 'linux-firefox', 'linux-chrome'], 'setViewportSize is not working in firefox', { silent: true });
                it(`size-${size}`, function() {
                    const selector = [`.AdaptiveWithAddingAndDeleting .Hermione-Item_size_${size}`];
                    const buttonMore = '.AdaptiveWithAddingAndDeleting .TabsMenu-Tab_more';
                    const lastMenuItem = '.AdaptiveWithAddingAndDeleting .Menu-Item:last-child';
                    const buttonAdd = '.AdaptiveWithAddingAndDeleting .ButtonAdd';
                    const buttonDelete = '.AdaptiveWithAddingAndDeleting .ButtonDelete';

                    return this.browser
                        .url('TabsMenu/hermione/hermione.html')
                        .setViewportSize({ width: 320, height: 1600 })
                        .assertView('all', selector)
                        .click(buttonAdd)
                        .assertView('with-more-button', selector)
                        .click(buttonMore)
                        .assertView('with-popup', selector)
                        .click(lastMenuItem)
                        .assertView('more-text', selector)
                        .click(buttonDelete)
                        .assertView('all-items', selector);
                });
            });
        });
    });
});
