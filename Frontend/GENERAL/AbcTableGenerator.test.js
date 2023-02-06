import React from 'react';
import { render } from 'enzyme';

import { AbcTableGenerator } from './AbcTableGenerator';

import tableMock from './sample-data.json';

describe('Should render Table', () => {
    it('with items, full house', () => {
        const wrapper = render(
            <AbcTableGenerator
                border
                highlight
                sticky
                className="Some-table"
                {...tableMock}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('without items', () => {
        const wrapper = render(
            <AbcTableGenerator headers={[]} tableRows={[]} />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
