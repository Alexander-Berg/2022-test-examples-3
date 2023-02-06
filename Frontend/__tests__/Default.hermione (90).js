describe('Toggle-Input', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Toggle%20Input&selectedStory=Default')
            .assertView('Default', selector);
    });
});
