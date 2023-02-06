describe('Problem-Settings---Checker', function () {
    it('TESTLIB_EXITCODE', function () {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problem%20Settings%20%7C%20Checker&selectedStory=TESTLIB_EXITCODE',
            )
            .assertView('TESTLIB_EXITCODE', selector);
    });
});
