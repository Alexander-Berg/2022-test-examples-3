describe('Hex-Area', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Hex%20Area&selectedStory=Default')
            .moveToObject(`${selector} > *`)
            .assertView('Default', selector);
    });
});
