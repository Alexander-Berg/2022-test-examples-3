describe('User-Profile', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=User%20Profile&selectedStory=Default')
            .assertView('Default', selector);
    });
});
