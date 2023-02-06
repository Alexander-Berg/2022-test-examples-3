import { waitUntilScroll } from '../../Scroller/tests/utils/waitUntilScroll';

hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('RichMediaGallery', function() {
        it('Скроллится кликом в стрелки', async function() {
            const bro = this.browser;
            await bro.yaOpenComponent('tests-richmediagallery--plain', true);

            await bro.click('.Scroller-ArrowButton_direction_right');
            await waitUntilScroll.call(bro, scrollLeft => scrollLeft > 0, 'карусель не подскроллилась');

            await bro.click('.Scroller-ArrowButton_direction_left');
            await waitUntilScroll.call(bro, scrollLeft => scrollLeft === 0, 'карусель не подскроллилась обратно');
        });
    });
});
