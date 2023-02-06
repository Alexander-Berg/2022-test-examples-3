describe('Select-List', function() {
    it('Scrolling-with-margin', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Select%20List&selectedStory=Scrolling-with-margin',
            )
            .moveToObject('body', -10, -10)
            .assertView('Scrolling-with-margin', selector);
    });
});
