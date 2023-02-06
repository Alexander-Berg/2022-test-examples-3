describe('Problems---Testsets', function () {
    it('Add-testset-started', function () {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problems%20%7C%20Testsets&selectedStory=Add-testset-started',
            )
            .assertView('Add-testset-started', selector);
    });
});
