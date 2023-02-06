describe('Contest-Settings---Monitor-additional', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20Monitor%20additional&selectedStory=default',
            )
            .assertView('default', selector);
    });
});
