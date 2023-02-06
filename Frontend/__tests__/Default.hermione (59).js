describe('Notice', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Notice&selectedStory=Default')
            .assertView('Default', selector);
    });
});
