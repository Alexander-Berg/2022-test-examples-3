describe('Permissions---List', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Permissions%20%7C%20List&selectedStory=default',
            )
            .assertView('default', selector);
    });
});
