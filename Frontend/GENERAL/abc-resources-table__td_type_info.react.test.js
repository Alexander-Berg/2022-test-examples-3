import React from 'react';
import { mount } from 'enzyme';

import AbcResourceTable__Td from 'b:abc-resources-table e:td m:type=info';

describe('AbcResourceTable__Td', () => {
    it('Should render table td of type info with link, name, id', () => {
        const resource = {
            external_id: '3491',
            link: 'https://racktables.yandex.net/?page=search&q=wiki-b2b.stable.qloud-b.yandex.net',
            name: 'wiki-b2b.stable.qloud-b.yandex.net'
        };

        const wrapper = mount(
            <table>
                <tbody>
                    <tr>
                        <AbcResourceTable__Td
                            key="info"
                            type="info"
                            resource={resource}
                    />
                    </tr>
                </tbody>
            </table>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render table td of type info without link, name, id', () => {
        const resource = {
            external_id: null,
            link: null,
            name: 'wiki-b2b.stable.qloud-b.yandex.net'
        };

        const wrapper = mount(
            <table>
                <tbody>
                    <tr>
                        <AbcResourceTable__Td
                            key="info"
                            type="info"
                            resource={resource}
                    />
                    </tr>
                </tbody>
            </table>
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
