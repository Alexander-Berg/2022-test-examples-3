describe('Icon', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Icon&selectedStory=Default')
            .assertView('Default', selector);
    });
});
