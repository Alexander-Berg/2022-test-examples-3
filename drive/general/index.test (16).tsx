import 'core-js';

import { shallow } from 'enzyme';
import { withHooks } from 'jest-react-hooks-shallow';
import React from 'react';

import { AddManageRolesModal } from './index';
import DoneCallback = jest.DoneCallback;
import { ERROR, SELECTED_ROLES, VALID_PROPS } from './mock';

describe('AddManageRolesModal', () => {
    it('Should render with valid roles data', () => {
        const wrapper = shallow(<AddManageRolesModal {...VALID_PROPS}/>);
        expect(wrapper).toMatchSnapshot();
    });

    it('Should render expected amount of roles', () => {
        const wrapper = shallow(<AddManageRolesModal {...VALID_PROPS}/>);
        expect(wrapper.find('.noTrHover').length).toEqual(SELECTED_ROLES.length);
    });

    it('Should disable copy button and print error if there is simulate error', (done: DoneCallback) => {
        withHooks(() => {
            fetchMock.mockRejectOnce(ERROR);
            const wrapper = shallow(<AddManageRolesModal {...VALID_PROPS}/>);
            setImmediate(() => {
                expect(wrapper).toMatchSnapshot();
                done();
            });
        });
    });
});
