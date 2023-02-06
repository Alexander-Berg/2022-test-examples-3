// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('Problemsets - Suggest', function() {
    it('fetch-error', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problemsets%20%7C%20Suggest&selectedStory=fetch-error',
            )
            .assertView('fetch-error', selector);
    });
});
