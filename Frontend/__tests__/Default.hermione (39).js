describe('Files-Tree', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Files%20Tree&selectedStory=Default')
            .assertView('Default', selector);
    });
});
