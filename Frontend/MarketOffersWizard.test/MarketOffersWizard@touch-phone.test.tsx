import React from 'react';
import { render } from 'enzyme';
import { withRegistry } from '@bem-react/di';
import { assertHasExactlyCountElements } from '@features/Market/Market.test/unitTestUtils';
import { getSnippetData, serpAdapterOptions } from '@features/Market/Market.utils/snippetData';
import type { IMarketOffersWizardProps } from '@features/Market/Market.features/MarketOffersWizard/MarketOffersWizard.typings';
import type { IMarketOffersWizardSnippet } from '@features/Market/Market.typings';
import { MarketOffersWizard as MarketOffersWizardBase } from '../MarketOffersWizard@touch-phone';
import { marketOffersWizardRegistry } from '../MarketOffersWizard.registry/touch-phone';
import { AdapterMarketOffersWizard as Base } from '../MarketOffersWizard@touch-phone.server';

// Нужен для тестирования protected-методов
class AdapterMarketOffersWizard extends Base {
    __getInitialState(): IMarketOffersWizardProps {
        return super.getInitialState() as IMarketOffersWizardProps;
    }
}

function getAdapter(itemsCount: number, expFlags: { [key: string]: number } = {}) {
    serpAdapterOptions.context.expFlags = {
        ...serpAdapterOptions.context.expFlags,
        ...expFlags,
    };

    const options = {
        ...serpAdapterOptions,
        snippet: getSnippetData({
            itemsCount,
        }) as unknown as IMarketOffersWizardSnippet,
    };

    return new AdapterMarketOffersWizard(options);
}

const MarketOffersWizard = withRegistry(marketOffersWizardRegistry)(MarketOffersWizardBase);

describe('MarketOffersWizard', () => {
    let adapter: AdapterMarketOffersWizard;

    it('Проверка отсутствия кнопки показать ещё', () => {
        adapter = getAdapter(6);

        const initialState = adapter.__getInitialState();
        initialState.productCardsShowcaseProps.numChildrenToRender = 6;

        const wizard = render(<MarketOffersWizard {...initialState} />);

        assertHasExactlyCountElements(wizard, '.ProductCard_more', 0);
        assertHasExactlyCountElements(wizard, '.ECommerceOffersUniSearch-ButtonMore', 0);
    });
});
