// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('Contest Problemset - Testing', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Problemset%20%7C%20Testing&selectedStory=default',
            )
            .assertView('default', selector, {
                windowSize: '1600x1024',
            });
    });
});
