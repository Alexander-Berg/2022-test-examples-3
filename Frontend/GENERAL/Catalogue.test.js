import React from 'react';
import { render } from 'enzyme';

import { Catalogue } from './Catalogue';

jest.mock('./components/Tree/Tree.container');
jest.mock('./components/Filters/Filters.container');
jest.mock('./components/Export/Export');
jest.mock('./components/Summary/Summary.container');

const permissions = {
    can_filter_and_click: true,
    can_export: true,
};

describe('Catalogue layout', () => {
    it('Should render catalogue layout', () => {
        const wrapper = render(
            <Catalogue
                permissions={permissions}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('Should render catalogue layout with root service defined', () => {
        const wrapper = render(
            <Catalogue
                permissions={permissions}
                rootServiceId={42}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('Should render catalogue layout without filters and summary', () => {
        const wrapper = render(
            <Catalogue
                permissions={{
                    ...permissions,
                    can_filter_and_click: false,
                }}
                rootServiceId={42}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('Should render catalogue layout without export', () => {
        const wrapper = render(
            <Catalogue
                permissions={{
                    ...permissions,
                    can_export: false,
                }}
                rootServiceId={42}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
