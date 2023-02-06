describe('Bubble-Pool', function () {
    it('Autoclosable', function () {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Bubble%20Pool&selectedStory=Autoclosable')
            .moveToObject('body', -10, -10)
            .assertView('Autoclosable', selector);
    });
});
