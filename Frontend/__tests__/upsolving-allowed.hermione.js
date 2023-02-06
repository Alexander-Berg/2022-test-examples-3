// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('Contest Settings - OutOfCompetition', function() {
    it('upsolving-allowed', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20OutOfCompetition&selectedStory=upsolving-allowed',
            )
            .assertView('upsolving-allowed', selector);
    });
});
