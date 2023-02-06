hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('NotificationList', function() {
        it('default', async function() {
            await this.browser.yaOpenComponent('tests-notificationlist--plain', true);
            await this.browser.yaAssertViewThemeStorybook('default', '.NotificationList');
        });
    });
});
