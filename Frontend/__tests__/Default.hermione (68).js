describe('Problems---Script', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Problems%20%7C%20Script&selectedStory=Default')
            .moveToObject(`${selector} > *`)
            .assertView('Default', selector);
    });
});
