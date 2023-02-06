import { assert } from 'chai';
import type { IMarketOffersWizardSnippet } from '@features/Market/Market.typings';
import type { IProductCardProps } from '@components/ProductCard/ProductCard.typings';
import { serpAdapterOptions, getSnippetData } from '@features/Market/Market.utils/snippetData';
import { AdapterMarketOffersWizard as Base } from '../MarketOffersWizard@desktop.server';

// Нужен для тестирования protected-методов
class AdapterMarketOffersWizard extends Base {
    __getInitialState() {
        return super.getInitialState();
    }
}

describe('AdapterMarketOffersWizard', () => {
    let adapter: AdapterMarketOffersWizard;

    beforeEach(() => {
        adapter = new AdapterMarketOffersWizard({
            ...serpAdapterOptions,
            snippet: getSnippetData({
                itemsCount: 6,
            }) as unknown as IMarketOffersWizardSnippet,
        });
    });

    describe('Без карусели маркетплейса', () => {
        it('Наличие данных для отображения дисклеймеров и рейтинга', () => {
            const initialState = adapter.__getInitialState();
            assert.isObject(initialState, 'Отсутствует initialState');
            if (!initialState) return;

            const { products } = initialState.productCardsShowcaseProps;
            assert.isArray(products, 'Отсутсвуют products');
            if (!products) return;

            const withRating = products.find((p: IProductCardProps) => typeof p.ratingValue !== 'undefined');
            const withDisclaimer = products.find((p: IProductCardProps) => p.disclaimer);

            assert.isString(withDisclaimer && withDisclaimer.disclaimer, 'Данные для дисклеймера не являются строкой');
            assert.isNumber(withRating && withRating.ratingValue, 'Данные для рейтинга не являются числом');
        });

        it('Один или два оффер, врезка есть', () => {
            adapter = new AdapterMarketOffersWizard({
                ...serpAdapterOptions,
                snippet: getSnippetData({
                    itemsCount: 1,
                }) as unknown as IMarketOffersWizardSnippet,
            });

            assert.isNotNull(adapter.__getInitialState());

            adapter = new AdapterMarketOffersWizard({
                ...serpAdapterOptions,
                snippet: getSnippetData({
                    itemsCount: 2,
                }) as unknown as IMarketOffersWizardSnippet,
            });

            assert.isNotNull(adapter.__getInitialState());
        });
    });
});
