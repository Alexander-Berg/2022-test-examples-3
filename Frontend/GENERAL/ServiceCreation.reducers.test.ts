import { initialState, serviceCreationReducer } from './ServiceCreation.reducers';
import {
    getMyRequestsRoutine,
    approveRequestRoutine,
    rejectRequestRoutine,
} from './ServiceCreation.actions';

import { getRequest } from '../components/Requests/testData/testData';
import { OnlyMineFilter } from '../components/Requests/Filters/Filters';

const myRequests = [getRequest(100), getRequest(200), getRequest(300)];

describe('Should handle actions', () => {
    it('getMyRequestsRoutine.SUCCESS', () => {
        expect(serviceCreationReducer(
            initialState,
            {
                type: getMyRequestsRoutine.SUCCESS,
                payload: {
                    results: myRequests,
                    totalPages: 10,
                },
            })).toEqual({ ...initialState, myRequests, myRequestsTotalPages: 10 },
        );
    });

    describe('approveRequestRoutine.SUCCESS', () => {
        it('Should update request with isApproved=true', () => {
            const requestId = 100;
            const myRequestsWithApproveRequest = myRequests.map(request => {
                if (requestId === request.id) {
                    return { ...request, isApproved: true };
                }

                return request;
            });

            expect(serviceCreationReducer(
                { ...initialState, myRequests },
                {
                    type: approveRequestRoutine.SUCCESS,
                    payload: {
                        requestId,
                        filter: OnlyMineFilter.DIRECT,
                    },
                })).toEqual({ ...initialState, myRequests: myRequestsWithApproveRequest });
        });
    });

    describe('rejectRequestRoutine.SUCCESS', () => {
        it('Should update request with isRejected=true', () => {
            const requestId = 100;
            const myRequestsWithApproveRequest = myRequests.map(request => {
                if (requestId === request.id) {
                    return { ...request, isRejected: true };
                }

                return request;
            });

            expect(serviceCreationReducer(
                { ...initialState, myRequests },
                {
                    type: rejectRequestRoutine.SUCCESS,
                    payload: {
                        requestId,
                        filter: OnlyMineFilter.DIRECT,
                    },
                })).toEqual({ ...initialState, myRequests: myRequestsWithApproveRequest });
        });
    });
});
