import React from 'react';
import { shallow } from 'enzyme';
import { getRequest } from '../testData/testData';
import { Table } from './Table';
import { AbcTableGenerator } from '../../../../../common/components/AbcTableGenerator/AbcTableGenerator';
import { ApproveRequest } from '../../../redux/types/requests';
import { Requester } from '../Requester/Requester';
import { Service } from '../Service/Service';
import { Approver } from '../Approver/Approver';

const columnNames = ['requester', 'service-incoming', 'approver-incoming'];

const headers = columnNames.map(columnsName => ({ key: columnsName, title: `i18n:${columnsName}` }));

const getTableRows = (requests: ApproveRequest[], onApprove: jest.Mock, onReject: jest.Mock) => {
    return requests.map(request => ({
        requester: request.requester ? <Requester requester={request.requester} /> : null,
        'service-incoming': <Service newService={request.service} newParentService={request.moveTo} />,
        'approver-incoming': (
            <Approver
                id={request.id}
                approver={request.approverIncoming}
                actions={request.actions}
                isApproved={request.isApproved}
                isRejected={request.isRejected}

                onApprove={onApprove}
                onReject={onReject}
            />
        ),
    }));
};

describe('Table', () => {
    it('Should have table component with correct props', () => {
        const onApprove = jest.fn();
        const onReject = jest.fn();
        const requests = [getRequest(1), getRequest(2), getRequest(3)];

        const wrapper = shallow(
            <Table
                requests={requests}
                onApprove={onApprove}
                onReject={onReject}
            />,
        );

        const tableComponent = wrapper.find(AbcTableGenerator);
        expect(tableComponent.prop('border')).toEqual(true);
        expect(tableComponent.prop('sticky')).toEqual(true);

        expect(tableComponent.prop('headers')).toEqual(headers);
        expect(tableComponent.prop('tableRows')).toEqual(getTableRows(requests, onApprove, onReject));
    });
});
