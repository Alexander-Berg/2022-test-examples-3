describe('Problem-Correct-Answers---Correct-answers', function() {
    it('one-answer', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problem%20Correct%20Answers%20%7C%20Correct%20Answers&selectedStory=one-answer',
            )
            .assertView('one-answer', selector, {
                screenshotTimeout: 5000,
            });
    });
});
