describe('Select-List', function() {
    it('Selected', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Select%20List&selectedStory=Selected')
            .moveToObject('body', -10, -10)
            .assertView('Selected', selector);
    });
});
