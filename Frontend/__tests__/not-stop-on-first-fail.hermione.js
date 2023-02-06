describe('Contest-Settings---Testing', function() {
    it('not-stop-on-first-fail', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20Testing&selectedStory=not-stop-on-first-fail',
            )
            .assertView('not-stop-on-first-fail', selector);
    });
});
