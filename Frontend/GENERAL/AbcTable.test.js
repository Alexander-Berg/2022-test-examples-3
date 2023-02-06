import React from 'react';
import { render } from 'enzyme';

import { AbcTable, THead, TBody, TFoot, Tr, Th, Td, Caption, } from './AbcTable';

describe('Should render Table', () => {
    it('empty', () => {
        const wrapper = render(
            <AbcTable />,
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with className', () => {
        const wrapper = render(
            <AbcTable className="table-class" />,
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('full house', () => {
        const wrapper = render(
            <AbcTable className="test-table-class" border sticky highlight>
                <Caption>Test table</Caption>
                <THead className="test-head-class">
                    <Tr><Th colSpan={2}>header</Th></Tr>
                </THead>
                <TBody className="test-body-class">
                    <Tr className="test-tr-class">
                        <Td className="test-td-class" rowSpan={2}>
                            this cell in two rows
                        </Td>
                        <Td>cell 1</Td>
                    </Tr>
                    <Tr>
                        <Td>cell 2</Td>
                    </Tr>
                </TBody>
                <TFoot>
                    <Tr>
                        <Td colSpan={2}>cell 3</Td>
                    </Tr>
                </TFoot>
            </AbcTable>,
        );

        expect(wrapper).toMatchSnapshot();
    });
});
