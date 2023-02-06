import React from 'react';
import { mount } from 'enzyme';
import inherit from 'inherit';

import AbcResourceRoles from 'b:abc-resource-roles';

inherit.self(AbcResourceRoles, {}, {
    getScope() {
        if (this.__base(...arguments)) {
            return this.__base(...arguments);
        }
        return document.body;
    }
});

describe('AbcResourceRoles', () => {
    it('Should render resource roles', () => {
        const wrapper = mount(
            <AbcResourceRoles
                text="text"
                onLinkClick={jest.fn()}
                popupOpen={false}
                onPopupOutsideClick={jest.fn()}
                isRolesDisabled={false}
                resourceRoles={[
                    {
                        name: { ru: 'tag_name' },
                        id: 'tag_slug'
                    }
                ]}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render disabled resource roles', () => {
        const wrapper = mount(
            <AbcResourceRoles
                text="text"
                onLinkClick={jest.fn()}
                popupOpen={false}
                onPopupOutsideClick={jest.fn()}
                isRolesDisabled
                resourceRoles={[]}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render empty resource roles', () => {
        const wrapper = mount(
            <AbcResourceRoles
                text="text"
                onLinkClick={jest.fn()}
                popupOpen={false}
                onPopupOutsideClick={jest.fn()}
                isRolesDisabled={false}
                resourceRoles={[]}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render loading resource roles', () => {
        const wrapper = mount(
            <AbcResourceRoles
                text="text"
                onLinkClick={jest.fn()}
                popupOpen={false}
                onPopupOutsideClick={jest.fn()}
                isRolesDisabled={false}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render resource roles with error', () => {
        const error = new Error();

        error.data = {
            message: {
                ru: 'Текст ru message',
                en: 'Текст en message'
            }
        };

        const wrapper = mount(
            <AbcResourceRoles
                error={error}
                text="text"
                onLinkClick={jest.fn()}
                popupOpen={false}
                onPopupOutsideClick={jest.fn()}
                isRolesDisabled={false}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('Should render resource roles with popup opened', () => {
        const wrapper = mount(
            <AbcResourceRoles
                text="text"
                onLinkClick={jest.fn()}
                popupOpen
                onPopupOutsideClick={jest.fn()}
                isRolesDisabled={false}
                resourceRoles={[
                    {
                        name: { ru: 'tag_name' },
                        id: 'tag_slug'
                    }
                ]}
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
