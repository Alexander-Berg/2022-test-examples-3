import React from 'react';
import { mount } from 'enzyme';
import inherit from 'inherit';

import __Field from 'b:abc-resource-view e:field m:type=approvers';
import Popup from 'b:popup2';

describe('abc-resource-view__field', () => {
    inherit.self(Popup, {
        _calcPos: () => ({ top: 0, right: 0, bottom: 0, left: 0 }),
    });

    it('Should render view field of type approvers > _maxSize', () => {
        const approvers = [{
            login: 'zomb-prj-282',
            name: { ru: 'Гермес Конрад' },
            is_dismissed: false,
        }, {
            login: 'zomb-prj-283',
            name: { ru: 'Гермес Конрад' },
            is_dismissed: false,
        }, {
            login: 'zomb-prj-284',
            name: { ru: 'Гермес Конрад' },
            is_dismissed: false,
        }];

        const wrapper = mount(
            <__Field
                key="field-type-approvers"
                type="approvers"
                label="label"
                approvers={approvers}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render view field of type approvers < _maxSize', () => {
        const approvers = [{
            login: 'zomb-prj-282',
            name: { ru: 'Гермес Конрад' },
            is_dismissed: false,
        }];

        const wrapper = mount(
            <__Field
                key="field-type-approvers"
                type="approvers"
                label="label"
                approvers={approvers}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render view field of type approvers = 0', () => {
        const approvers = [];

        const wrapper = mount(
            <__Field
                key="field-type-approvers"
                type="approvers"
                label="label"
                approvers={approvers}
            />
        );

        expect(wrapper).toMatchSnapshot();

        expect(wrapper.state().noOnePopupOpen).toEqual(false);
        wrapper.find('.abc-resource-view__no-one-link').simulate('click');
        expect(wrapper.state().noOnePopupOpen).toEqual(true);

        wrapper.unmount();
    });
});
