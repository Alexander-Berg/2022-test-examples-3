describe('Problem-Settings---Checker', function () {
    it('disabled', function () {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problem%20Settings%20%7C%20Checker&selectedStory=disabled',
            )
            .assertView('disabled', selector);
    });
});
