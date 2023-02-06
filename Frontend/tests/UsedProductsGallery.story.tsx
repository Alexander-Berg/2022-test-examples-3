import React from 'react';
import { number, withKnobs } from '@storybook/addon-knobs';

import { withStaticRouter } from '@src/storybook/decorators/withStaticRouter';
import { createOffersStub } from '@src/storybook/stubs/entitites/createOffersStub';
import { StubReduxProvider } from '@src/storybook/stubs/StubReduxProvider';
import { createPlatformStories } from '@src/storybook/utils/createPlatformStories';
import { getEntitiesInitialState } from '@src/store/services/entities/reducer';

import type { ISearchResult } from '@src/store/services/pages/search/types';
import type { IEntitiesState } from '@src/store/services/entities/types';
import type { IOffer } from '@src/typings/offer';

import type { IUsedProductsGalleryProps } from '../UsedProductsGallery.typings';
import { UsedProductsGallery } from '..';
import './UsedProductsGallery.story.scss';

function createOffers<T>(length = 8) {
    const offers: Record<string, IOffer> = {};
    const results: ISearchResult[] = [];

    return Array.from<T>({ length }).reduce((out, _, index) => {
        const id = index.toString(36);
        const usedGoods = true;
        const usedGoodsInfo = {
            publishDate: '2021-03-15T12:24:15.251000+00:00',
            sellerName: 'Артем Колобков',
        };

        out.offers[id] = createOffersStub({ id, usedGoods, usedGoodsInfo });
        out.results.push({ id, type: 'offer', showUid: '', reqid: '' });
        return out;
    }, { offers, results });
}

const { offers, results } = createOffers();

const entities: IEntitiesState = {
    ...getEntitiesInitialState(),
    offers,
};

const defaultProps: IUsedProductsGalleryProps = {
    className: 'UsedProductsGalleryStory',
    items: results,
};

createPlatformStories('Tests/UsedProductsGallery', UsedProductsGallery, stories => {
    stories
        .addDecorator(withKnobs)
        .addDecorator(withStaticRouter())
        .add('plain', Component => {
            const limit = number('limit', 12);
            const props = { ...defaultProps };

            props.items = props.items.slice(0, limit);

            return (
                <StubReduxProvider stub={{ entities }}>
                    <Component {...props} />
                </StubReduxProvider>
            );
        });
});
