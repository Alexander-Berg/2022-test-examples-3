describe('Problem-Settings---Checker', function () {
    it('double', function () {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problem%20Settings%20%7C%20Checker&selectedStory=double',
            )
            .assertView('double', selector);
    });
});
