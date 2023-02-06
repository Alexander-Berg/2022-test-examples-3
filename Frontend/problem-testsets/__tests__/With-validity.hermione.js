describe('Problems---Testsets', function() {
    it('With-validity', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problems%20%7C%20Testsets&selectedStory=With-validity',
            )
            .assertView('With-validity', selector);
    });
});
