describe('Problem-Correct-Answers---Correct-answers', function() {
    it('empty', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problem%20Correct%20Answers%20%7C%20Correct%20Answers&selectedStory=empty',
            )
            .assertView('empty', selector, {
                screenshotTimeout: 5000,
            });
    });
});
