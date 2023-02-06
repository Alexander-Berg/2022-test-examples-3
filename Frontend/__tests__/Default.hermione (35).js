describe('Errors---Internal-Server', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Errors%20%7C%20Internal%20Server&selectedStory=Default',
            )
            .assertView('Default', selector);
    });
});
