import { waitUntilScroll } from './utils/waitUntilScroll';

hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('Scroller', function() {
        it('Внешний вид', async function() {
            const bro = this.browser;
            await bro.yaOpenComponent('tests-scroller--plain', true);
            await bro.yaScrollElement('.Scroller-ItemsScroller', 102, 0);
            await bro.yaAssertViewThemeStorybook('plain', '.Scroller');
        });

        it('Имеет скроллящийся контейнер', async function() {
            const bro = this.browser;
            await bro.yaOpenComponent('tests-scroller--plain', true);

            await bro.yaScrollElement('.Scroller-ItemsScroller', 102, 0);
            await waitUntilScroll.call(bro, scrollLeft => scrollLeft === 102, 'карусель не скроллится');
        });
    });
});
