describe('Contest-Settings---Access', function() {
    it('opened-after-end', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20Access&selectedStory=opened-after-end',
            )
            .assertView('opened-after-end', selector);
    });
});
