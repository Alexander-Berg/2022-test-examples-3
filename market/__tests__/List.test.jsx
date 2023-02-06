import React from 'react';
import { create } from 'react-test-renderer';

import List from '../index';
import responseWithOffers from '../../../../fixtures/response-with-offers';
import responseWithModel from '../../../../fixtures/response-with-model';

describe('Price', () => {
    beforeAll(() => {
        window.screen.orientation = {
            type: 'landscape',
            addEventListener: jest.fn(),
        };
    });

    test('with model found', () => {
        const { offers } = responseWithModel;
        const c = create(<List isModelFound items={offers} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('without model', () => {
        const { offers } = responseWithOffers;
        const c = create(<List isModelFound={false} items={offers} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('with model found (tile)', () => {
        const { offers } = responseWithModel;
        const c = create(<List isModelFound items={offers} isTiles />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('without model (tile)', () => {
        const { offers } = responseWithOffers;
        const c = create(<List isModelFound={false} items={offers} isTiles />);
        expect(c.toJSON()).toMatchSnapshot();
    });
});
