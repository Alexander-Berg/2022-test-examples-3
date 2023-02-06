describe('Problems---Validators', function() {
    it('Run-all-validators', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problems%20%7C%20Validators&selectedStory=Run-all-validators',
            )
            .assertView('Run-all-validators', selector);
    });
});
