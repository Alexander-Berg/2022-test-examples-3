// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('Review Card', function() {
    it('Single', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Review%20Card&selectedStory=Single')
            .assertView('Single', selector);
    });
});
