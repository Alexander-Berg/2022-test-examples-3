// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('Hint', function() {
    it('With-label', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Hint&selectedStory=With-label')
            .assertView('With-label', selector);
    });
});
