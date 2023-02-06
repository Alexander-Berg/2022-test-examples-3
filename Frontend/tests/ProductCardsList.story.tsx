import React from 'react';
import { composeU } from '@bem-react/core';

import { withStaticRouter } from '@src/storybook/decorators/withStaticRouter';
import type { ISearchResult } from '@src/store/services/pages/search/types';
import { createEntitiesStub } from '@src/storybook/stubs/entitites/createEntitiesStub';
import { createOffersStub } from '@src/storybook/stubs/entitites/createOffersStub';
import { createProductStub } from '@src/storybook/stubs/entitites/createProductStub';
import { StubReduxProvider } from '@src/storybook/stubs/StubReduxProvider';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import type { IExactPrice, IRangePrice } from '@src/typings/price';
import type { IProduct } from '@src/typings/product';
import { ProductCardPresenter } from '@src/components/ProductCard';
import { ProductCardsList as ProductCardsListBase, withViewVertical, withViewHorizontal } from '../index';
import type { IProductCardsListProps } from '../ProductCardsList.typings';

const ProductCardsList = composeU(withViewVertical, withViewHorizontal)(
    ProductCardsListBase as React.FC<IProductCardsListProps<ISearchResult>>,
);

const exactPrice: IExactPrice = {
    type: 'exact',
    current: '13451',
    old: '42312',
    currency: 'RUR',
};

const rangePrice: IRangePrice = {
    type: 'range',
    max: '42312',
    min: '13451',
    currency: 'RUR',
};

function createProducts(length = 4, offers = defaultOffers): IProduct[] {
    return Array.from({ length }).map((_, i) => {
        const offerId = i.toString(35);
        offers.push(offerId);
        return createProductStub(
            i.toString(36),
            [offerId],
            i % 4 !== 0 ? exactPrice : rangePrice,
        );
    });
}

const defaultOffers: string[] = [];
const defaultProducts = createProducts();
const defaultEntries = createEntitiesStub(
    defaultProducts,
    defaultOffers,
    id => createOffersStub({ id, price: exactPrice }),
);
const renderCard = (item: ISearchResult) => (
    <ProductCardPresenter id={item.id} type={item.type} showUid="" reqid={item.reqid} />
);

const defaultProps: React.ComponentProps<typeof ProductCardsList> = {
    view: 'vertical',
    renderCard,
    items: defaultProducts.map(x => ({ id: x.id, type: 'product', showUid: '', reqid: '' })),
    requestStatus: 'success',
    loadMoreRequestStatus: 'initial',
};

createPlatformStories('Tests/ProductCardsList', ProductCardsList, stories => {
    stories
        .addDecorator(withStaticRouter())
        .add('narrow', ProductCardsList => {
            return (
                <StubReduxProvider stub={{ entities: defaultEntries }}>
                    <ProductCardsList {...defaultProps} />
                </StubReduxProvider>
            );
        })
        .add('wide', ProductCardsList => {
            const offers: string[] = [];
            const products = createProducts(5, offers);
            const entities = createEntitiesStub(
                products,
                offers,
                id => createOffersStub({ id, price: exactPrice }),
            );
            const items: ISearchResult[] = products.map(x => ({ id: x.id, type: 'product', showUid: '', reqid: '' }));

            return (
                <StubReduxProvider stub={{ entities }}>
                    <ProductCardsList
                        items={items}
                        type="wide"
                        view="vertical"
                        renderCard={renderCard}
                        requestStatus="success"
                        loadMoreRequestStatus="initial"
                    />
                </StubReduxProvider>
            );
        })
        .add('search-loading', ProductCardsList => {
            const offers: string[] = [];
            const products = createProducts(5, offers);
            const entities = createEntitiesStub(
                products,
                offers,
                id => createOffersStub({ id, price: exactPrice }),
            );
            const items: ISearchResult[] = products.map(x => ({ id: x.id, type: 'product', showUid: '', reqid: '' }));

            return (
                <StubReduxProvider stub={{ entities }}>
                    <ProductCardsList
                        items={items}
                        view="vertical"
                        requestStatus="loading"
                        loadMoreRequestStatus="initial"
                        renderCard={renderCard}
                    />
                </StubReduxProvider>
            );
        })
        .add('loading-more', ProductCardsList => {
            const offers: string[] = [];
            const products = createProducts(4, offers);
            const entities = createEntitiesStub(
                products,
                offers,
                id => createOffersStub({ id, price: exactPrice }),
            );
            const items: ISearchResult[] = products.map(x => ({ id: x.id, type: 'product', showUid: '', reqid: '' }));

            return (
                <StubReduxProvider stub={{ entities }}>
                    <ProductCardsList
                        items={items}
                        view="vertical"
                        requestStatus="success"
                        loadMoreRequestStatus="loading"
                        renderCard={renderCard}
                    />
                </StubReduxProvider>
            );
        })
        .add('horizontal', ProductCardsList => {
            return (
                <StubReduxProvider stub={{ entities: defaultEntries }}>
                    <ProductCardsList {...defaultProps} view="horizontal" />
                </StubReduxProvider>
            );
        });
});
