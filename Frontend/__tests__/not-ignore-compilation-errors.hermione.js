describe('Contest-Settings---Monitor-additional', function() {
    it('not-ignore-compilation-errors', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20Monitor%20additional&selectedStory=not-ignore-compilation-errors',
            )
            .assertView('not-ignore-compilation-errors', selector);
    });
});
