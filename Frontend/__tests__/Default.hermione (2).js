describe('Add-Problems', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Add%20Problems&selectedStory=Default')
            .assertView('Default', selector);
    });
});
