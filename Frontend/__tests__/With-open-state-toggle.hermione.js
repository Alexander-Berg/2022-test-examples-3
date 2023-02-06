describe('Accordion', function() {
    it('With-open-state-toggle', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Accordion&selectedStory=With-open-state-toggle',
            )
            .moveToObject(`${selector} > *`)
            .assertView('With-open-state-toggle', selector);
    });
});
