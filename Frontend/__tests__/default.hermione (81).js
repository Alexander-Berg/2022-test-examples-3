// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('Section Nav', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Section%20Nav&selectedStory=default')
            .assertView('default', selector);
    });
});
