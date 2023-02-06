// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('Contest Members - Main', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Members%20%7C%20Main&selectedStory=Default',
            )
            .assertView('Default', selector);
    });
});
