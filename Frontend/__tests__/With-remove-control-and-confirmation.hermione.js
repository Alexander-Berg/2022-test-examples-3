describe('Accordion', function() {
    it('With-remove-control-and-confirmation', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Accordion&selectedStory=With-remove-control-and-confirmation',
            )
            .assertView('With-remove-control-and-confirmation', selector);
    });
});
