import { cls } from '../PromotionIncut.const';

hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('PromotionIncut', function() {
        const cnRoot = `.${cls.root}`;

        it('default', async function() {
            const bro = this.browser;

            await bro.yaOpenComponent('tests-promotionincut--showcase', true);
            await bro.yaAssertViewThemeStorybook('default', cnRoot);

            await bro.yaOpenComponent(
                'tests-promotionincut--showcase',
                true,
                [
                    { name: 'withIcon', value: 'false' },
                    { name: 'withClose', value: 'false' },
                ],
            );
            await bro.yaAssertViewThemeStorybook('without_icon_and_close', cnRoot);
        });

        it('theme', async function() {
            const bro = this.browser;

            await bro.yaOpenComponent(
                'tests-promotionincut--showcase',
                true,
                [{ name: 'theme', value: 'purple' }],
            );
            await bro.yaAssertViewThemeStorybook('theme_purple', cnRoot);
            await bro.yaOpenComponent(
                'tests-promotionincut--showcase',
                true,
                [{ name: 'theme', value: 'red' }],
            );
            await bro.yaAssertViewThemeStorybook('theme_red', cnRoot);
        });
    });
});
