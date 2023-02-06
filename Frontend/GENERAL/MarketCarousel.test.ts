import { assert } from 'chai';
import type { IProductCardProps } from '@components/ProductCard/ProductCard.typings';
import type { IDirectOffer } from '../../Market.typings';
import { AdapterMarketCarousel } from './MarketCarousel.server';

abstract class AdapterMarketCarouselTest extends AdapterMarketCarousel {
    public processDirectItem(item: IDirectOffer): IProductCardProps {
        return super.processDirectItem(item);
    }

    protected sendLogs() {}
}

const sampleDirectOffer: IDirectOffer = {
    text: {
        name: 'Кроссовки Nike',
    },
    click_url: {
        general: 'https://yabs.yandex.ru/count/HASH',
    },
    images: {},
    domain: 'brandshop.ru',
    object_id: 777,
};

describe('MarketCarousel.server', () => {
    it('Не показываем старую цену из динамических объявлений если она <= новой цены', () => {
        const directOffer: IDirectOffer = {
            ...sampleDirectOffer,
            dynamic_add_info: {
                currency: 'RUB',
                price: '2000',
                old_price: '1000',
            },
        };

        assert.propertyVal(
            AdapterMarketCarouselTest.prototype.processDirectItem(directOffer),
            'oldPrice',
            undefined,
        );
    });

    it('Показываем старую цену из динамических объявлений если она > новой цены', () => {
        const directOffer: IDirectOffer = {
            ...sampleDirectOffer,
            dynamic_add_info: {
                currency: 'RUB',
                price: '2000',
                old_price: '5000',
            },
        };

        assert.propertyVal(
            AdapterMarketCarouselTest.prototype.processDirectItem(directOffer),
            'oldPrice',
            5000,
        );
    });

    it('Не показываем старую цену из обычных объявлений если она <= новой цены', () => {
        const directOffer: IDirectOffer = {
            ...sampleDirectOffer,
            price_info: {
                currency: 'RUB',
                current: '2000',
                old: '1000',
            },
        };

        assert.propertyVal(
            AdapterMarketCarouselTest.prototype.processDirectItem(directOffer),
            'oldPrice',
            undefined,
        );
    });

    it('Показываем старую цену из обычных объявлений если она > новой цены', () => {
        const directOffer: IDirectOffer = {
            ...sampleDirectOffer,
            price_info: {
                currency: 'RUB',
                current: '2000',
                old: '5000',
            },
        };

        assert.propertyVal(
            AdapterMarketCarouselTest.prototype.processDirectItem(directOffer),
            'oldPrice',
            5000,
        );
    });

    describe('Дисклеймеры', () => {
        it('Медицинский дисклеймер не в тултипе', () => {
            const directOffer: IDirectOffer = {
                ...sampleDirectOffer,
                bannerFlags: 'medicine',
            };

            assert.propertyVal(
                AdapterMarketCarouselTest.prototype.processDirectItem(directOffer),
                'disclaimerInTooltip',
                false,
            );
        });

        it('Алкогольный дисклеймер не в тултипе', () => {
            const directOffer: IDirectOffer = {
                ...sampleDirectOffer,
                bannerFlags: 'alcohol',
            };

            assert.propertyVal(
                AdapterMarketCarouselTest.prototype.processDirectItem(directOffer),
                'disclaimerInTooltip',
                false,
            );
        });

        it('БАД дисклеймер не в тултипе', () => {
            const directOffer: IDirectOffer = {
                ...sampleDirectOffer,
                bannerFlags: 'dietarysuppl',
            };

            assert.propertyVal(
                AdapterMarketCarouselTest.prototype.processDirectItem(directOffer),
                'disclaimerInTooltip',
                false,
            );
        });

        it('Не медицинский дисклеймер в тултипе', () => {
            const directOffer: IDirectOffer = {
                ...sampleDirectOffer,
                bannerFlags: 'annoying',
            };

            assert.propertyVal(
                AdapterMarketCarouselTest.prototype.processDirectItem(directOffer),
                'disclaimerInTooltip',
                true,
            );
        });
    });
});
