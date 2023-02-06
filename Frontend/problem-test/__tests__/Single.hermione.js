describe('Problems---Test-Answer', function() {
    it('Single', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problems%20%7C%20Test%20Answer&selectedStory=Single',
            )
            .assertView('Single', selector);
    });
});
