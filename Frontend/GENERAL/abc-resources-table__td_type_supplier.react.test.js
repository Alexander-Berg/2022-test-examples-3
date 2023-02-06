import React from 'react';
import { shallow } from 'enzyme';

import AbcResourcesTable__Td from 'b:abc-resources-table e:td m:type=supplier';

describe('AbcResourcesTable__Td', () => {
    it('Should render empty abc-resources-table__td_type_supplier', () => {
        const wrapper = shallow(
            <AbcResourcesTable__Td
                type="supplier"
                tags={[]}
                supplier={{ slug: 'foo', name: { ru: 'bar' } }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render abc-resources-table__td_type_supplier', () => {
        const wrapper = shallow(
            <AbcResourcesTable__Td
                type="supplier"
                tags={[
                    {
                        name: { ru: 'foo' }
                    }
                ]}
                supplier={{ slug: 'foo', name: { ru: 'bar' } }}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
