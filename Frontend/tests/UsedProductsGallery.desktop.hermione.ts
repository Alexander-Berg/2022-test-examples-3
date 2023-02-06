import { waitUntilScroll } from '@src/components/Scroller/tests/utils/waitUntilScroll';

hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('UsedProductsGallery', function() {
        it('Скроллится кликом в стрелки', async function() {
            const bro = this.browser;
            await bro.yaOpenComponent('tests-usedproductsgallery--plain', true);

            await bro.click('.Scroller-ArrowButton_direction_right');
            await waitUntilScroll.call(bro, scrollLeft => scrollLeft > 0, 'карусель не подскроллилась');

            await bro.click('.Scroller-ArrowButton_direction_left');
            await waitUntilScroll.call(bro, scrollLeft => scrollLeft === 0, 'карусель не подскроллилась обратно');
        });
    });
});
