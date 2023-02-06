import React from 'react';
import { create } from 'react-test-renderer';

import About from '../index';

describe('About', () => {
    test('Pricebar opened, About visible', () => {
        const c = create(
            <About isPricebarVisible isPricebarActive isAboutVisible setAboutVisible={() => {}} data={() => {}} />,
        );
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('Pricebar opened, About not visible', () => {
        const c = create(
            <About isPricebarVisible isPricebarActive isAboutVisible={false} setAboutVisible={() => {}} />,
        );
        expect(c.toJSON()).toMatchSnapshot();
    });

    test('Pricebar closed, About not visible', () => {
        const c = create(
            <About
                isPricebarVisible={false}
                isPricebarActive={false}
                isAboutVisible={false}
                setAboutVisible={() => {}}
            />,
        );
        expect(c.toJSON()).toMatchSnapshot();
    });
});
