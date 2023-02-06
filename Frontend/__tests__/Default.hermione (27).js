describe('Contest-Submissions---Statistics', function () {
    it('Default', function () {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Submissions%20%7C%20Statistics&selectedStory=Default',
            )
            .assertView('Default', selector);
    });
});
