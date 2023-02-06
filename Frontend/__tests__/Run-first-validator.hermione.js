describe('Problems---Validators', function() {
    it('Run-first-validator', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problems%20%7C%20Validators&selectedStory=Run-first-validator',
            )
            .assertView('Run-first-validator', selector);
    });
});
