// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('Select List Modal', function() {
    it('Header', function() {
        const selector = '.select-list-content';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Select%20List%20Modal&selectedStory=Header')
            .assertView('Header', selector);
    });
});
