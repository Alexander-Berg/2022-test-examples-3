describe('Problems---Validators', function() {
    it('With-validation-results', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problems%20%7C%20Validators&selectedStory=With-validation-results',
            )
            .assertView('With-validation-results', selector);
    });
});
