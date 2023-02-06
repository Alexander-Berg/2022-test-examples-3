describe('Problem-Settings---Match-sets-answer', function () {
    it('Disabled', function () {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problem%20Settings%20%7C%20Match%20sets%20answer&selectedStory=Disabled',
            )
            .assertView('Disabled', selector);
    });
});
