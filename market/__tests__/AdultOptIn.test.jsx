import React from 'react';
import { create } from 'react-test-renderer';

import AdultOptIn from '../index';

describe('AdultOptIn', () => {
    test('visible', () => {
        const c = create(
            <AdultOptIn isAdultOptInVisible setAdultOptInVisible={() => {}} setPricebarVisible={() => {}} />,
        );
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('hidden', () => {
        const c = create(
            <AdultOptIn isAdultOptInVisible={false} setAdultOptInVisible={() => {}} setPricebarVisible={() => {}} />,
        );
        expect(c.toJSON()).toMatchSnapshot();
    });
});
