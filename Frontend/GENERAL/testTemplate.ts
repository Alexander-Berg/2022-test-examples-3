const testTemplate = (component: string, story: string) => {
    let testName = component.trim().replace(/[\s|]/g, '-');
    return `describe('${testName}', function() {
    it('${story}', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=${encodeURIComponent(
                component,
            )}&selectedStory=${story}')
            .assertView('${story}', selector);
    });
});`;
};

export default testTemplate;
