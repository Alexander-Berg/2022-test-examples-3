describe('Problem-Settings---Files', function () {
    it('Default', function () {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problem%20Settings%20%7C%20Files&selectedStory=Default',
            )
            .assertView('Default', selector);
    });
});
