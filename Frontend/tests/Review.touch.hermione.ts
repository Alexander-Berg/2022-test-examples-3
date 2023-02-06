describe('Storybook', function() {
    describe('Review', function() {
        it('default', async function() {
            const bro = this.browser;

            await bro.yaOpenComponent('tests-reviews-review--plain', true);

            await bro.yaAssertViewThemeStorybook('plain', '.Review');
        });

        it('Короткое имя автора без аватарки', async function() {
            const bro = this.browser;

            await bro.yaOpenComponent('tests-reviews-review--plain', true, [
                { name: 'name', value: 'Василий П.' },
                { name: 'avatarId', value: 'fake' },
            ]);

            await bro.yaAssertViewThemeStorybook('short-author', '.Review');
        });

        it('Длинный текст', async function() {
            const bro = this.browser;

            await bro.yaOpenComponent('tests-reviews-review--plain', true, [
                { name: 'text', value: 'Достоинства: • Мне всё понравилось, менеджер был вежлив и участлив\n• Товары надёжно упакованы\n• Пункт выдачи легко найти\n• Интерьер приятный и стильный\n• Было просто забрать заказ\n• Заказ был в пункте выдачи в назначенный день, я получил его ровно в срок и в хорошей упаковке\n\nКомментарий: Лампа ловушка для комаров и мух выглядит впечатляюще... Комариков ловит... и убивает.' },
            ]);

            await bro.yaAssertViewThemeStorybook('long-text', '.Review');

            await bro.click('.TextCut-More');

            await bro.yaAssertViewThemeStorybook('long-text-expanded', '.Review');
        });
    });
});
