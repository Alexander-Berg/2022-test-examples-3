import React from 'react';
import { shallow } from 'enzyme';
import { ErrorMessages } from './ErrorMessages';
import { Message } from '../../../../../common/components/Message/Message';

describe('ErrorMessages', () => {
    describe('All errors exist', () => {
        const requestsError = { status: 404, data: { detail: 'Not Found' } };
        const approveError = { status: 403, data: { detail: 'Forbidden' } };
        const rejectError = { status: 400, data: { detail: 'Bad Request' } };

        const wrapper = shallow(
            <ErrorMessages
                requestsError={requestsError}
                approveError={approveError}
                rejectError={rejectError}
            />,
        );

        it('Should have requests error with correct props', () => {
            const requestsErrorMessage = wrapper.find('.Requests-Message_reason_requests');
            expect(requestsErrorMessage).toHaveLength(1);
            expect(requestsErrorMessage.prop('type')).toEqual('error');
            expect(requestsErrorMessage.prop('view')).toEqual('new');

            const requestsErrorBlock = requestsErrorMessage.find(Message.Error);
            expect(requestsErrorBlock.prop('data')).toEqual(requestsError.data);
        });

        it('Should have approve error with correct props', () => {
            const approveErrorMessage = wrapper.find('.Requests-Message_reason_approveLink');
            expect(approveErrorMessage).toHaveLength(1);
            expect(approveErrorMessage.prop('type')).toEqual('error');
            expect(approveErrorMessage.prop('view')).toEqual('new');

            const approveErrorBlock = approveErrorMessage.find(Message.Error);
            expect(approveErrorBlock.prop('data')).toEqual(approveError.data);
        });

        it('Should have reject error with correct props', () => {
            const rejectErrorMessage = wrapper.find('.Requests-Message_reason_rejectLink');
            expect(rejectErrorMessage).toHaveLength(1);
            expect(rejectErrorMessage.prop('type')).toEqual('error');
            expect(rejectErrorMessage.prop('view')).toEqual('new');

            const rejectErrorBlock = rejectErrorMessage.find(Message.Error);
            expect(rejectErrorBlock.prop('data')).toEqual(rejectError.data);
        });
    });

    describe('No errors', () => {
        const wrapper = shallow(
            <ErrorMessages
                requestsError={null}
                approveError={null}
                rejectError={null}
            />,
        );

        it('Should have no requests error', () => {
            const requestsErrorMessage = wrapper.find('.Requests-Message_reason_requests');
            expect(requestsErrorMessage).toHaveLength(0);
        });

        it('Should have no approve error', () => {
            const approveErrorMessage = wrapper.find('.Requests-Message_reason_approveLink');
            expect(approveErrorMessage).toHaveLength(0);
        });

        it('Should have no reject error', () => {
            const rejectErrorMessage = wrapper.find('.Requests-Message_reason_rejectLink');
            expect(rejectErrorMessage).toHaveLength(0);
        });
    });
});
