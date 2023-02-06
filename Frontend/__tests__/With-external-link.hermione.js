// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('Bubble', function() {
    it('With-external-link', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Bubble&selectedStory=With-external-link')
            .assertView('With-external-link', selector);
    });
});
