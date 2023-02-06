describe('Async-Operations', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Async%20Operations&selectedStory=Default')
            .assertView('Default', selector);
    });
});
