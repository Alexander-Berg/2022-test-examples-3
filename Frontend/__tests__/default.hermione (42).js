describe('FilterButton', function() {
    const selector = '.story-container';

    it('default', function() {
        return this.browser
            .url('storybook/iframe.html?selectedKind=FilterButton&selectedStory=default')
            .assertView('default', selector, { testTimeout: 3000 })
            .moveToObject(`${selector} div > *`)
            .assertView('hovered', selector);
    });
});
