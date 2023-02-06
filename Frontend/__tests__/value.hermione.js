describe('Datetime-Picker', function() {
    it('value', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Datetime%20Picker&selectedStory=value')
            .moveToObject(`${selector} > *`)
            .assertView('value', selector);
    });
});
