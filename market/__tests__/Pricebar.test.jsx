import React from 'react';
import { create } from 'react-test-renderer';

import Pricebar from '../index';
import responseWithOffers from '../../../../fixtures/response-with-offers';
import responseWithModel from '../../../../fixtures/response-with-model';
import { getPricebarData } from '../../../utils/get-data';

global.fetch = jest.fn(() => Promise.resolve());

describe.skip('Pricebar', () => {
    test('with model and offers: closed state', () => {
        const pricebarData = getPricebarData(responseWithModel);

        const c = create(
            <Pricebar
                isOptOutVisible={false}
                setOptOutVisible={() => {}}
                active={false}
                visible
                setVisible={() => {}}
                setAboutVisible={() => {}}
                setActive={() => {}}
                data={pricebarData}
            />,
        );
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('with model and offers: opened state', () => {
        const pricebarData = getPricebarData(responseWithModel);

        const c = create(
            <Pricebar
                isOptOutVisible={false}
                setOptOutVisible={() => {}}
                active
                visible
                setVisible={() => {}}
                setAboutVisible={() => {}}
                setActive={() => {}}
                data={pricebarData}
            />,
        );
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('with offers and without model: closed state', () => {
        const pricebarData = getPricebarData(responseWithOffers);

        const c = create(
            <Pricebar
                isOptOutVisible={false}
                setOptOutVisible={() => {}}
                active={false}
                visible
                setVisible={() => {}}
                setAboutVisible={() => {}}
                setActive={() => {}}
                data={pricebarData}
            />,
        );
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('with offers and without model: opened state', () => {
        const pricebarData = getPricebarData(responseWithOffers);

        const c = create(
            <Pricebar
                isOptOutVisible={false}
                setOptOutVisible={() => {}}
                active
                visible
                setVisible={() => {}}
                setAboutVisible={() => {}}
                setActive={() => {}}
                data={pricebarData}
            />,
        );
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('with offers tile and without model: opened state', () => {
        const pricebarData = getPricebarData(responseWithOffers);
        pricebarData.isVisual = true;

        const c = create(
            <Pricebar
                isOptOutVisible={false}
                setOptOutVisible={() => {}}
                active
                visible
                setVisible={() => {}}
                setAboutVisible={() => {}}
                setActive={() => {}}
                data={pricebarData}
            />,
        );
        expect(c.toJSON()).toMatchSnapshot();
    });
});
