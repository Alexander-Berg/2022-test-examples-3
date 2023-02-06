describe('Problem-Settings---Checker', function () {
    it('EJUDGE_EXITCODE', function () {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problem%20Settings%20%7C%20Checker&selectedStory=EJUDGE_EXITCODE',
            )
            .assertView('EJUDGE_EXITCODE', selector);
    });
});
