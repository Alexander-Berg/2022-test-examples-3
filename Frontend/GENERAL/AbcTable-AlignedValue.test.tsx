import React from 'react';
import { render } from 'enzyme';

import {
    AbcTable, Tr,
    AbcTableAlignedValue as AlignedValue,
} from '..';

describe('AbcTable-AlignedValue', () => {
    it('Render with all data', () => {
        const wrapper = render(
            <AbcTable>
                <Tr>
                    <AlignedValue
                        value={123.45}
                        unit="GB"
                    />
                </Tr>
            </AbcTable>,
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('Render with minimal data', () => {
        const wrapper = render(
            <AbcTable>
                <Tr>
                    <AlignedValue value={123} />
                </Tr>
            </AbcTable>,
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('Should propagate provided cell props to all cells', () => {
        const wrapper = render(
            <AbcTable>
                <Tr>
                    <AlignedValue
                        value={123}
                        className="custom-class-name"
                        colSpan={42}
                    />
                </Tr>
            </AbcTable>,
        );

        expect(wrapper).toMatchSnapshot();
    });
});
