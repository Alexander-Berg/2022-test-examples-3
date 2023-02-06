import React from 'react';

import { shallow, mount } from 'enzyme';
import { TaUsername } from '../../../../Dispenser/Dispenser.features/components/Username/TaUsername';

import { Approver } from './Approver';
import { getUser } from '../testData/testData';
import { RequestActions } from '../../../redux/types/requests';

describe('Approver', () => {
    describe('Full information', () => {
        const user = getUser(100);

        const wrapper = mount(
            <Approver
                id={1}
                approver={user}
                actions={[RequestActions.approve, RequestActions.reject]}

                isApproved={false}
                isRejected={false}

                onApprove={jest.fn()}
                onReject={jest.fn()}
            />,
        );

        afterEach(() => {
            wrapper.unmount();
        });

        it('Should render full information', () => {
            const taUsername = wrapper.find(TaUsername);
            expect(taUsername).toHaveLength(1);
            expect(taUsername.prop('dismissed')).toEqual(user.isDismissed);
            expect(taUsername.prop('href')).toEqual(`//staff.yandex-team.ru/${user.login}`);
            expect(taUsername.prop('username')).toEqual(user.login);
            expect(taUsername.prop('children')).toEqual(user.name.ru);

            const approveLink = wrapper.find('.Requests-DecisionLink_type_approve');
            expect(approveLink.hostNodes()).toHaveLength(1);

            const approvedLink = wrapper.find('.Requests-DecisionLink_approved');
            expect(approvedLink.hostNodes()).toHaveLength(0);

            const rejectLink = wrapper.find('.Requests-DecisionLink_type_reject');
            expect(rejectLink.hostNodes()).toHaveLength(1);

            const rejectedLink = wrapper.find('.Requests-DecisionLink_rejected');
            expect(rejectedLink.hostNodes()).toHaveLength(0);
        });

        it('Should render approved link without reject link', () => {
            wrapper.setProps({ isApproved: true });

            const approvedLink = wrapper.find('.Requests-DecisionLink_approved');
            expect(approvedLink.hostNodes()).toHaveLength(1);

            const rejectLink = wrapper.find('.Requests-DecisionLink_type_reject');
            expect(rejectLink.hostNodes()).toHaveLength(0);
        });

        it('Should render rejectedLink link without approve link', () => {
            wrapper.setProps({ isApproved: false, isRejected: true });

            const rejectedLink = wrapper.find('.Requests-DecisionLink_rejected');
            expect(rejectedLink.hostNodes()).toHaveLength(1);

            const approveLink = wrapper.find('.Requests-DecisionLink_type_approve');
            expect(approveLink.hostNodes()).toHaveLength(0);
        });
    });

    describe('No information', () => {
        const wrapper = shallow(
            <Approver
                id={1}
                actions={[]}

                isApproved={false}
                isRejected={false}

                onApprove={jest.fn()}
                onReject={jest.fn()}
            />,
        );

        it('Should render - without approver person', () => {
            const taUsername = wrapper.find(TaUsername);
            expect(taUsername).toHaveLength(0);

            const approverBlock = wrapper.find('.Requests-ApproverPerson');
            expect(approverBlock.text()).toEqual('—');
        });

        it('Should not render approve link', () => {
            const approveLink = wrapper.find('.Requests-DecisionLink_type_approve');
            expect(approveLink).toHaveLength(0);
        });

        it('Should not render reject link', () => {
            const rejectLink = wrapper.find('.Requests-DecisionLink_type_reject');
            expect(rejectLink).toHaveLength(0);
        });
    });

    describe('Check click handlers', () => {
        const approveHandler = jest.fn();
        const rejectHandler = jest.fn();

        const wrapper = mount(
            <Approver
                id={1}
                actions={[RequestActions.approve, RequestActions.reject]}

                isApproved={false}
                isRejected={false}

                onApprove={approveHandler}
                onReject={rejectHandler}
            />,
        );

        afterAll(() => {
            wrapper.unmount();
        });

        it('Should call onApprove after approve link click', () => {
            const approveLink = wrapper.find('.Requests-DecisionLink_type_approve').hostNodes();

            approveLink.simulate('click');
            expect(approveHandler).toHaveBeenCalledTimes(1);
        });

        it('Should call onReject after reject link click', () => {
            const rejectLink = wrapper.find('.Requests-DecisionLink_type_reject').hostNodes();

            rejectLink.simulate('click');
            expect(rejectHandler).toHaveBeenCalledTimes(1);
        });
    });
});
