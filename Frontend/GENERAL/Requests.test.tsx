import React from 'react';
import { shallow } from 'enzyme';

import { Requests } from './Requests';
import { Filters, OnlyMineFilter } from './Filters/Filters';
import { ErrorMessages } from './ErrorMessages/ErrorMessages';
import { requests } from './testData/testData';
import { Table } from './Table/Table';

export const commonProps = {
    getMyRequests: jest.fn(),
    getMyRequestsWithDescendants: jest.fn(),
    updateQueryStr: jest.fn(),
    onApprove: jest.fn(),
    onReject: jest.fn(),
    setOnlyMineFilter: jest.fn(),

    requests,
    queryObj: {},
    totalPages: 1,
    page: 1,

    requestsLoading: false,
    requestsError: null,
    approveError: null,
    rejectError: null,

    onlyMineFilter: OnlyMineFilter.DIRECT,
};

describe('Requests', () => {
    it('Should render default', () => {
        const page = 2;
        const totalPages = 2;
        const queryObj = { page: 2 };

        const setOnlyMineFilter = jest.fn();
        const onApprove = jest.fn();
        const onReject = jest.fn();
        const updateQueryStr = jest.fn();

        const wrapper = shallow(
            <Requests
                {...commonProps}
                setOnlyMineFilter={setOnlyMineFilter}

                onApprove={onApprove}
                onReject={onReject}

                page={page}
                totalPages={totalPages}
                queryObj={queryObj}
                updateQueryStr={updateQueryStr}
            />,
        );

        expect(wrapper.find('.Requests')).toHaveLength(1);

        const filters = wrapper.find(Filters);
        expect(filters).toHaveLength(1);
        expect(filters.prop('onlyMineFilter')).toEqual(OnlyMineFilter.DIRECT);
        expect(filters.prop('onChange')).toEqual(setOnlyMineFilter);

        const errorMessages = wrapper.find(ErrorMessages);
        expect(errorMessages).toHaveLength(1);
        expect(errorMessages.prop('requestsError')).toEqual(null);
        expect(errorMessages.prop('approveError')).toEqual(null);
        expect(errorMessages.prop('rejectError')).toEqual(null);

        const table = wrapper.find(Table);
        expect(table).toHaveLength(1);
        expect(table.prop('requests')).toEqual(requests);
        expect(table.prop('onApprove')).toEqual(onApprove);
        expect(table.prop('onReject')).toEqual(onReject);

        expect(wrapper.find('.Requests-Message_reason_empty')).toHaveLength(0);

        const pagination = wrapper.findWhere(n => n.name() === 'WithBemMod(AbcPagination)[type:pages]');
        expect(pagination).toHaveLength(1);
        expect(pagination.prop('currentPage')).toEqual(page);
        expect(pagination.prop('totalPages')).toEqual(totalPages);
        expect(pagination.prop('pageParam')).toEqual('page');
        expect(pagination.prop('className')).toEqual('Requests-Pagination');
    });

    it('Should show spin without tableInfo when requests are loading', () => {
        const wrapper = shallow(
            <Requests
                {...commonProps}
                requestsLoading
            />,
        );

        const tableInfo = wrapper.find('.Requests-TableInfo');
        expect(tableInfo).toHaveLength(0);

        const spin = wrapper.find('.Requests-Spin');
        expect(spin).toHaveLength(1);
        expect(spin.prop('progress')).toEqual(true);
        expect(spin.prop('view')).toEqual('default');
        expect(spin.prop('size')).toEqual('m');
        expect(spin.prop('position')).toEqual('center');
    });

    it('Should show empty message if requests are empty', () => {
        const wrapper = shallow(
            <Requests
                {...commonProps}
                requests={[]}
            />,
        );

        const emptyMessage = wrapper.find('.Requests-Message_reason_empty');
        expect(emptyMessage).toHaveLength(1);
    });

    it('Should show requests error', () => {
        const requestsError = { status: 100, data: { detail: 'Error with requests' } };
        const approveError = null;
        const rejectError = { status: 103, data: { detail: 'Error with reject decision' } };

        const wrapper = shallow(
            <Requests
                {...commonProps}
                requestsError={requestsError}
                approveError={approveError}
                rejectError={rejectError}
            />,
        );

        const errorMessage = wrapper.find(ErrorMessages);
        expect(errorMessage).toHaveLength(1);

        expect(errorMessage.prop('requestsError')).toEqual(requestsError);
        expect(errorMessage.prop('approveError')).toEqual(approveError);
        expect(errorMessage.prop('rejectError')).toEqual(rejectError);
    });
});
