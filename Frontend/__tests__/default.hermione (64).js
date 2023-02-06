describe('Popup', function() {
    const openButton = '.open-button';

    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Popup&selectedStory=default')
            .assertView('default', selector)
            .click(openButton)
            .assertView('opened_popup', selector);
    });
});
