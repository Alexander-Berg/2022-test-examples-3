import SliderAdapter from '../Slider.adapter';

describe('SliderAdapter', () => {
    describe('getItems()', () => {
        it('для theme=ecom-banner должен ограничивать количество элементов восьмью', () => {
            // @ts-ignore
            const items = new SliderAdapter({}).getItems({
                theme: 'ecom-banner',
                // @ts-ignore
                items: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10],
            });
            expect(items).toHaveLength(8);
        });
    });
});
