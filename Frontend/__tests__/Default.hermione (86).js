describe('Suffix-Value', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Suffix%20Value&selectedStory=Default')
            .assertView('Default', selector);
    });
});
