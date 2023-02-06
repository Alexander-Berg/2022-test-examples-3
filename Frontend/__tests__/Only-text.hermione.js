// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('Bubble', function() {
    it('Only-text', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Bubble&selectedStory=Only-text')
            .assertView('Only-text', selector);
    });
});
