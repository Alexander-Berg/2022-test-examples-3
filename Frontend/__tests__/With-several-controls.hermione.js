describe('Accordion', function() {
    it('With-several-controls', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Accordion&selectedStory=With-several-controls')
            .assertView('With-several-controls', selector);
    });
});
