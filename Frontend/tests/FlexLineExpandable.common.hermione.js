describe('Storybook', function() {
    describe('FlexLineExpandable', function() {
        beforeEach(async function() {
            await this.browser.yaOpenComponent('tests-flexlineexpandable--plain', true);
        });

        it('plain', async function() {
            await this.browser.yaAssertViewThemeStorybook('plain', '.FlexStory');
        });

        it('expanded', async function() {
            const bro = this.browser;
            await bro.yaWaitForVisible('.FlexStory-More');
            await bro.click('.FlexStory-More');
            await bro.yaAssertViewThemeStorybook('expanded', '.FlexStory');
        });
    });
});
