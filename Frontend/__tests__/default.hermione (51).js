describe('Link', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Link&selectedStory=default')
            .assertView('default', selector);
    });
});
