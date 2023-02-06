import React from 'react';
import { shallow } from 'enzyme';

import { AccessRoles } from './AccessRoles';

describe('Should render AccessRoles', () => {
    describe('for staff', () => {
        it('two roles', () => {
            const wrapper = shallow(<AccessRoles type="staff" accessRoles={['abc_ext_role_viewer', 'abc_ext_role_self_viewer']} />);
            expect(wrapper).toMatchSnapshot();
        });

        it('first role', () => {
            const wrapper = shallow(<AccessRoles type="staff" accessRoles={['abc_ext_role_viewer']} />);
            expect(wrapper).toMatchSnapshot();
        });

        it('second role', () => {
            const wrapper = shallow(<AccessRoles type="staff" accessRoles={['abc_ext_role_self_viewer']} />);
            expect(wrapper).toMatchSnapshot();
        });
    });

    describe('for department', () => {
        it('two roles', () => {
            const wrapper = shallow(<AccessRoles type="department" accessRoles={['abc_ext_role_viewer', 'abc_ext_role_self_viewer']} />);
            expect(wrapper).toMatchSnapshot();
        });

        it('first role', () => {
            const wrapper = shallow(<AccessRoles type="department" accessRoles={['abc_ext_role_viewer']} />);
            expect(wrapper).toMatchSnapshot();
        });

        it('second role', () => {
            const wrapper = shallow(<AccessRoles type="department" accessRoles={['abc_ext_role_self_viewer']} />);
            expect(wrapper).toMatchSnapshot();
        });
    });

    it('incorrect type', () => {
        const wrapper = shallow(<AccessRoles type="aa" accessRoles={['abc_ext_role_viewer', 'abc_ext_role_self_viewer']} />);
        expect(wrapper).toMatchSnapshot();
    });
});
