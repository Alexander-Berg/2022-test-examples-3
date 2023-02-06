describe('Select-List', function() {
    it('Scrolling', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Select%20List&selectedStory=Scrolling')
            .moveToObject('body', -10, -10)
            .assertView('Scrolling', selector);
    });
});
