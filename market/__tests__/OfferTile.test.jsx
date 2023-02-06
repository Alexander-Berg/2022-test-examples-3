import React from 'react';
import { create } from 'react-test-renderer';

import Offer from '../index';
import responseWithOffers from '../../../../../fixtures/response-with-offers';
import responseWithModel from '../../../../../fixtures/response-with-model';
import offerWithoutImageUrl from '../../../../../fixtures/offer/without-image-url';
import offerWithDiscount from '../../../../../fixtures/offer/with-discount';
import offerWithLongNameAndShopName from '../../../../../fixtures/offer/with-long-name-and-shop-name';

describe('Offer', () => {
    test('(tile) with all data and model found', () => {
        const { offers } = responseWithModel;
        const [offer] = offers;

        const c = create(<Offer isModelFound data={offer} isTile />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('(tile) with all data and without model', () => {
        const { offers } = responseWithOffers;
        const [offer] = offers;

        const c = create(<Offer isModelFound data={offer} isTile />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('(tile) without image url', () => {
        const c = create(<Offer isModelFound data={offerWithoutImageUrl} isTile />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('(tile) with model, old price and discount', () => {
        const c = create(<Offer isModelFound data={offerWithDiscount} isTile />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('(tile) with all data and long shop name', () => {
        const c = create(<Offer isModelFound data={offerWithLongNameAndShopName} isTile />);
        expect(c.toJSON()).toMatchSnapshot();
    });
});
