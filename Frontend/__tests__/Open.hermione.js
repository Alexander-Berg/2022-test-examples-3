describe('Spoiler', function () {
    it('Open', function () {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Spoiler&selectedStory=Open')
            .moveToObject(`${selector} > *`)
            .assertView('Open', selector);
    });
});
