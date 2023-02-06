describe('Contest-Settings---Create-Contest', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20Create%20Contest&selectedStory=default',
            )
            .assertView('default', selector);
    });
});
