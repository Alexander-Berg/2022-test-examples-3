describe('Submission---Submission', function () {
    it('Default', function () {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Submission%20%7C%20Submission&selectedStory=Default',
            )
            .assertView('Default', selector);
    });
});
