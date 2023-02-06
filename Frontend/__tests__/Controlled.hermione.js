// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('Hint', function() {
    it('Controlled', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Hint&selectedStory=Controlled')
            .assertView('Controlled', selector);
    });
});
