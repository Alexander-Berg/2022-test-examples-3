import React from 'react';
import { shallow } from 'enzyme';

import AbcResourcesTable__Td from 'b:abc-resources-table e:td m:type=consumer-tags';

describe('AbcResourcesTable__Td', () => {
    it('Should render empty abc-resources-table__td_type_consumer-tags', () => {
        const wrapper = shallow(
            <AbcResourcesTable__Td
                type="consumer-tags"
                tags={[]}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render abc-resources-table__td_type_consumer-tags', () => {
        const wrapper = shallow(
            <AbcResourcesTable__Td
                type="consumer-tags"
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
