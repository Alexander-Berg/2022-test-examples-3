import React from 'react';
import { create } from 'react-test-renderer';

import OfferContent from '../index';
import fullOffer from '../../../../fixtures/offer/full';
import withAgeDisclaimer from '../../../../fixtures/offer/with-age-disclaimer';
import withoutDiscountAndDisclaimer from '../../../../fixtures/offer/without-discount-and-disclaimer';
import shopInMarket from '../../../../fixtures/offer/shop-in-market';

describe('OfferContent', () => {
    test('with base price, discount', () => {
        const c = create(<OfferContent offer={fullOffer} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('with age disclaimer', () => {
        const c = create(<OfferContent offer={withAgeDisclaimer} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('without discount and discllaimer', () => {
        const c = create(<OfferContent offer={withoutDiscountAndDisclaimer} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('shop in market', () => {
        const c = create(<OfferContent offer={shopInMarket} />);
        expect(c.toJSON()).toMatchSnapshot();
    });
});
