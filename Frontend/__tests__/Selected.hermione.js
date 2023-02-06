describe('Add-Problems', function() {
    it('Selected', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Add%20Problems&selectedStory=Selected')
            .assertView('Selected', selector);
    });
});
