describe('Contest-Settings---Verdict', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20Verdict&selectedStory=default',
            )
            .pause(500)
            .assertView('default', selector);
    });
});
