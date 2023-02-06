describe('Datetime-Picker', function() {
    it('only-date', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Datetime%20Picker&selectedStory=only-date')
            .moveToObject(`${selector} > *`)
            .assertView('only-date', selector);
    });
});
