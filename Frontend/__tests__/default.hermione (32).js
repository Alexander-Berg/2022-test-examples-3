describe('Datetime-Picker', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Datetime%20Picker&selectedStory=default')
            .moveToObject(`${selector} > *`)
            .assertView('default', selector);
    });
});
