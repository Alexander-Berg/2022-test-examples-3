describe('Problemset-Settings---Problems', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problemset%20Settings%20%7C%20Problems&selectedStory=default',
            )
            .assertView('default', selector);
    });
});
