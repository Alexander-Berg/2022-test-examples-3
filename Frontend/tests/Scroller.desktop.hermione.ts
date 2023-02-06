import { waitUntilScroll } from './utils/waitUntilScroll';

hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('Scroller', function() {
        it('Скроллится кликом в стрелки', async function() {
            const bro = this.browser;
            await bro.yaOpenComponent('tests-scroller--plain', true);

            await bro.click('.Scroller-ArrowButton_direction_right');
            await waitUntilScroll.call(bro, scrollLeft => scrollLeft === 102, 'карусель не подскроллилась на ширину одного элемента');

            await bro.click('.Scroller-ArrowButton_direction_left');
            await waitUntilScroll.call(bro, scrollLeft => scrollLeft === 0, 'карусель не подскроллилась обратно в начало');
        });
    });
});
