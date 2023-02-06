describe('Table', function() {
    it('With-Draggable-Rows', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Table&selectedStory=With-Draggable-Rows')
            .assertView('With-Draggable-Rows', selector);
    });
});
