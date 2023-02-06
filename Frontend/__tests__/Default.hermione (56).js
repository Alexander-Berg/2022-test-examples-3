describe('Message', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Message&selectedStory=Default')
            .assertView('Default', selector);
    });
});
