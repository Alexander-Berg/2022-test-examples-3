describe('Logo', function() {
    it('English', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Logo&selectedStory=English')
            .assertView('English', selector);
    });
});
