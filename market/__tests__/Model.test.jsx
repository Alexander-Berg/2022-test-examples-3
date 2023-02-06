import React from 'react';
import { create } from 'react-test-renderer';

import Model from '../index';
import responseWithModel from '../../../../fixtures/response-with-model';
import { getPricebarData } from '../../../utils/get-data';

describe('Model', () => {
    test('with all data', () => {
        const { model } = getPricebarData(responseWithModel);

        const c = create(<Model isModelFound data={model} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('without rating badge value', () => {
        const { model } = getPricebarData(responseWithModel);
        model.rating = undefined;

        const c = create(<Model isModelFound data={model} />);
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('with disclaimer', () => {
        const { model } = getPricebarData(responseWithModel);
        model.disclaimer = 'warning';

        const c = create(<Model isModelFound data={model} />);
        expect(c.toJSON()).toMatchSnapshot();
    });
});
