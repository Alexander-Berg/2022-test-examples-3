describe('Storybook', function() {
    describe('TextCut', function() {
        it('Внешний вид', async function() {
            const bro = this.browser;
            await bro.yaOpenComponent('tests-textcut--plain', true);
            await bro.yaWaitForVisible('.TextCut');
            await bro.yaAssertViewThemeStorybook('plain', '.TextCut');
        });

        it('Разворачивание текста', async function() {
            const bro = this.browser;
            await bro.yaOpenComponent('tests-textcut--enablehidetext', true);
            await bro.yaWaitForVisible('.TextCut');

            await bro.click('.TextCut-More');
            await bro.yaWaitForVisible('.TextCut-Collapsed');
            await bro.yaAssertViewThemeStorybook('plain', '.TextCut');

            await bro.click('.TextCut-Collapsed');
            await bro.yaWaitForVisible('.TextCut-More');
        });
    });
});
