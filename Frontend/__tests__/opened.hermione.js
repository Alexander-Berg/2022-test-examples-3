describe('Contest-Settings---Access', function() {
    it('opened', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20Access&selectedStory=opened',
            )
            .assertView('opened', selector);
    });
});
