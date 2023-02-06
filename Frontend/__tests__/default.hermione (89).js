// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('Time Picker', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Time%20Picker&selectedStory=default')
            .assertView('default', selector);
    });
});
