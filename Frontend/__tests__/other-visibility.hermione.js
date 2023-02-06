describe('Contest-Settings---Monitor', function() {
    it('other-visibility', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20Monitor&selectedStory=other-visibility',
            )
            .assertView('other-visibility', selector);
    });
});
