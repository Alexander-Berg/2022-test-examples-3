describe('Errors---Common', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Errors%20%7C%20Common&selectedStory=Default')
            .assertView('Default', selector);
    });
});
