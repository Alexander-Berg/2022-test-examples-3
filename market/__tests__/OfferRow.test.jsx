import React from 'react';
import { create } from 'react-test-renderer';

import Offer from '../index';
import responseWithOffers from '../../../../../fixtures/response-with-offers';
import responseWithModel from '../../../../../fixtures/response-with-model';
import offerWithoutImageUrl from '../../../../../fixtures/offer/without-image-url';
import offerWithDiscount from '../../../../../fixtures/offer/with-discount';
import offerWithLongNameAndShopName from '../../../../../fixtures/offer/with-long-name-and-shop-name';
import offerWithShopInMarket from '../../../../../fixtures/offer/shop-in-market';

describe('Offer', () => {
    test('(row) with all data and model found', () => {
        const { offers } = responseWithModel;
        const [offer] = offers;

        const c = create(<Offer isModelFound data={offer} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('(row) with all data and without model', () => {
        const { offers } = responseWithOffers;
        const [offer] = offers;

        const c = create(<Offer isModelFound data={offer} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('(row) el without image url', () => {
        const c = create(<Offer isModelFound data={offerWithoutImageUrl} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('(row) el with model, old price and discount', () => {
        const c = create(<Offer isModelFound data={offerWithDiscount} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('(row) el with all data and long shop name', () => {
        const c = create(<Offer isModelFound data={offerWithLongNameAndShopName} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('(row) el with all data and a shop in the market', () => {
        const c = create(<Offer isModelFound data={offerWithShopInMarket} />);
        expect(c.toJSON()).toMatchSnapshot();
    });
});
