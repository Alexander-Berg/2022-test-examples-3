describe('ListRoll', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=ListRoll&selectedStory=Default')
            .assertView('Default', selector);
    });
});
