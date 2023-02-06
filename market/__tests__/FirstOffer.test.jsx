import React from 'react';
import { create } from 'react-test-renderer';

import FirstOffer from '../index';

import offerWithDiscount from '../../../../fixtures/offer/with-discount';
import withAgeDisclaimer from '../../../../fixtures/offer/with-age-disclaimer';
import offerWithoutDiscountAndDisclaimer from '../../../../fixtures/offer/without-discount-and-disclaimer';
import fullOffer from '../../../../fixtures/offer/full';

describe('FirstOffer', () => {
    test('with base price, discount and age disclaimer', () => {
        const c = create(<FirstOffer data={fullOffer} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('without base price and discount, age disclaimer', () => {
        const c = create(<FirstOffer data={offerWithoutDiscountAndDisclaimer} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('with base price and discount, without disclaimer', () => {
        const c = create(<FirstOffer data={offerWithDiscount} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('with age disclaimer, without discount and base', () => {
        const c = create(<FirstOffer data={withAgeDisclaimer} />);
        expect(c.toJSON()).toMatchSnapshot();
    });
});
