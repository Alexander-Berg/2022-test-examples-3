import React from 'react';
import { create } from 'react-test-renderer';

import ModelPopup from '../index';
import responseWithModel from '../../../../fixtures/response-with-model';
import { getPricebarData } from '../../../utils/get-data';

describe('ModelPopup', () => {
    beforeAll(() => {
        window.screen.orientation = {
            type: 'landscape',
            addEventListener: jest.fn(),
        };
    });

    test('with all data', () => {
        const pricebarData = getPricebarData(responseWithModel);
        const { model = {}, offers = [], links = {}, disclaimer = '' } = pricebarData;

        const c = create(
            <ModelPopup isModelFound disclaimer={disclaimer} model={model} offers={offers} links={links} />,
        );
        expect(c.toJSON()).toMatchSnapshot();
    });
});
