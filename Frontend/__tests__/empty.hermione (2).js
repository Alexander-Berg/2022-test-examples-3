// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('Contest Settings - ContestSettings', function() {
    it('empty', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20ContestSettings&selectedStory=empty',
            )
            .assertView('empty', selector);
    });
});
