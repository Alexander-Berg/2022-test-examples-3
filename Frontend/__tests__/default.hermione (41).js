describe('Filter', function() {
    const selector = '.story-container';
    const openButton = '.filter-menu__open-button';
    const textMenuButton = '.filter-menu__group:nth-child(1) .filter-menu__item';
    const selectMenuButton = '.filter-menu__group:nth-child(2) .filter-menu__item';
    const booleanMenuButton = '.filter-menu__group:nth-child(3) .filter-menu__item';

    it('default', function() {
        return this.browser
            .url('storybook/iframe.html?selectedKind=Filter&selectedStory=default')
            .assertView('default', selector)
            .click(openButton)
            .assertView('opened_fields_menu', selector)
            .click(textMenuButton)
            .assertView('opened_text_menu', selector)
            .click(selectMenuButton)
            .assertView('opened_select_menu', selector)
            .click(booleanMenuButton)
            .assertView('opened_boolean_menu', selector);
    });
});
