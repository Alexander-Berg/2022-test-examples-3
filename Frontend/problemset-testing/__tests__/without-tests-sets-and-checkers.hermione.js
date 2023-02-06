// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('Contest Problemset - Testing', function() {
    it('without-tests-sets-and-checkers', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Problemset%20%7C%20Testing&selectedStory=without-tests-sets-and-checkers',
            )
            .assertView('without-tests-sets-and-checkers', selector, {
                windowSize: '1600x1024',
            });
    });
});
