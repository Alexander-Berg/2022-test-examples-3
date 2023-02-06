describe('Problem-Settings---Checker', function () {
    it('TESTLIB', function () {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problem%20Settings%20%7C%20Checker&selectedStory=TESTLIB',
            )
            .assertView('TESTLIB', selector);
    });
});
