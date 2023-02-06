// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('Problemset Settings - ProblemsetSettings', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problemset%20Settings%20%7C%20ProblemsetSettings&selectedStory=default',
            )
            .assertView('default', selector);
    });
});
