describe('Select-List', function() {
    it('Simple', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Select%20List&selectedStory=Simple')
            .moveToObject('body', -10, -10)
            .assertView('Simple', selector);
    });
});
