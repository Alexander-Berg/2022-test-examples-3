describe('Contest-Settings---Monitor-Description', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20Monitor%20Description&selectedStory=default',
            )
            .assertView('default', selector);
    });
});
