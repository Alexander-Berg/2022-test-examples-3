import { render } from 'enzyme';
import React from 'react';
import { Ticker } from './Ticker';

describe('Ticker', () => {
    it('Should render Ticker with value', () => {
        const wrapper = render(
            <Ticker
                count={3}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
