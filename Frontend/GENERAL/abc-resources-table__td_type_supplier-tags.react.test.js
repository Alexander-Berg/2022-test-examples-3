import React from 'react';
import { shallow } from 'enzyme';

import AbcResourcesTable__Td from 'b:abc-resources-table e:td m:type=supplier-tags';

describe('AbcResourcesTable__Td', () => {
    it('Should render empty abc-resources-table__td_type_supplier-tags', () => {
        const wrapper = shallow(
            <AbcResourcesTable__Td
                type="supplier-tags"
                tags={[]}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render abc-resources-table__td_type_supplier-tags', () => {
        const wrapper = shallow(
            <AbcResourcesTable__Td
                type="supplier-tags"
                tags={[
                    {
                        name: { ru: 'foo' }
                    }
                ]}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
