import React from 'react';
import { mount } from 'enzyme';

// @ts-ignore
import { SCOPE_PREFIX } from 'tools-access-react-redux-router/src/configs';

import { withRedux } from '~/src/common/hoc';
import { configureStore } from '~/src/abc/react/redux/store';

import { RequestsContainer } from './Requests.container';
import { OnlyMineFilter } from './Filters/Filters';
import { Requests } from './Requests';

export const commonProps = {
    onApprove: jest.fn(),
    onReject: jest.fn(),

    requests: [],
    queryObj: {},
    totalPages: 1,

    requestsLoading: false,
    requestsError: null,
    approveError: null,
    rejectError: null,
};

describe('RequestsContainer', () => {
    const getMyRequests = jest.fn();
    const updateQueryStr = jest.fn();

    const store = configureStore({
        initialState: {
            [SCOPE_PREFIX]: '',
        },
        fetcherOptions: {
            fetch: () => Promise.resolve(),
        },
    });

    const RequestsConnected = withRedux(RequestsContainer, store);

    const wrapper = mount(
        <RequestsConnected
            {...commonProps}

            getMyRequests={getMyRequests}
            updateQueryStr={updateQueryStr}
            onlyMineFilter={OnlyMineFilter.DIRECT}
            page={2}
        />,
    );

    it('Should get my requests at the beginning', () => {
        expect(getMyRequests).toHaveBeenCalledWith({ page: 2, filter: OnlyMineFilter.DIRECT });
    });

    it('Should get my requests if page have changed', () => {
        wrapper.setProps({ page: 3 });

        expect(getMyRequests).toHaveBeenCalledWith({ page: 3, filter: OnlyMineFilter.DIRECT });
    });

    it('Should get my requests if filter have changed', () => {
        wrapper.setProps({ onlyMineFilter: OnlyMineFilter.HIERARCHY });

        expect(getMyRequests).toHaveBeenCalledWith({ page: 3, filter: OnlyMineFilter.HIERARCHY });
    });

    it('Should change page on 1 when filter is switching', () => {
        const setOnlyMineFilter = wrapper.find(Requests).prop('setOnlyMineFilter') as (value: OnlyMineFilter) => void;
        setOnlyMineFilter(OnlyMineFilter.DIRECT);

        expect(updateQueryStr).toHaveBeenCalledWith({ only_mine: OnlyMineFilter.DIRECT, page: 1 });
    });
});
