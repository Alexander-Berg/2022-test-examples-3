import React from 'react';
import { create } from 'react-test-renderer';

import OffersPopup from '../index';
import responseWithModel from '../../../../fixtures/response-with-model';
import responseWithOffers from '../../../../fixtures/response-with-offers';
import { getPricebarData } from '../../../utils/get-data';

describe('OffersPopup', () => {
    beforeAll(() => {
        window.screen.orientation = {
            type: 'landscape',
            addEventListener: jest.fn(),
        };
    });

    test('with all data and model found', () => {
        const pricebarData = getPricebarData(responseWithModel);
        const { links, offers } = pricebarData;
        const c = create(<OffersPopup isModelFound links={links} offers={offers} />);

        expect(c.toJSON()).toMatchSnapshot();
    });

    test('with all data and without model', () => {
        const pricebarData = getPricebarData(responseWithOffers);
        const { links, offers } = pricebarData;
        const c = create(<OffersPopup isModelFound={false} links={links} offers={offers} />);

        expect(c.toJSON()).toMatchSnapshot();
    });
});
