import {
    selectQueryObj,
    selectPage,
    selectOnlyMineFilter,
    selectRequests,
    selectRequestsTotalPages,
} from './ServiceCreation.selectors';
import { initialState } from './ServiceCreation.reducers';
import { OnlyMineFilter } from '../components/Requests/Filters/Filters';
import { getRequest } from '../components/Requests/testData/testData';
import { ApproveRequest } from './types/requests';
import { StoreForServiceCreation } from './types/store';

const store: StoreForServiceCreation = {
    serviceCreation: initialState,
    'tools-access-react-redux-router': '',
};

describe('selectors', () => {
    describe('selectQueryObj', () => {
        it('Should select parameters correctly', () => {
            expect(selectQueryObj({ ...store, 'tools-access-react-redux-router': '/?page=1' })).toEqual({ page: ['1'] });
        });
    });

    describe('selectPage', () => {
        it('Should select page', () => {
            expect(selectPage({ ...store, 'tools-access-react-redux-router': '/?page=1' })).toEqual(1);
        });

        it('Should get 1 if page is not a parameter', () => {
            expect(selectPage({ ...store, 'tools-access-react-redux-router': '/' })).toEqual(1);
        });

        it('Should select the last page parameter', () => {
            expect(selectPage({ ...store, 'tools-access-react-redux-router': '/?page=1&page=2' })).toEqual(2);
        });

        it('Should get 1 if page is not a number', () => {
            expect(selectPage({ ...store, 'tools-access-react-redux-router': '/?page=2jfjf' })).toEqual(1);
        });
    });

    describe('selectOnlyMineFilter', () => {
        it('Should select direct filter', () => {
            expect(selectOnlyMineFilter({ ...store, 'tools-access-react-redux-router': '/?only_mine=direct' }))
                .toEqual(OnlyMineFilter.DIRECT);
        });

        it('Should select hierarchy filter', () => {
            expect(selectOnlyMineFilter({ ...store, 'tools-access-react-redux-router': '/?only_mine=hierarchy' }))
                .toEqual(OnlyMineFilter.HIERARCHY);
        });

        it('Should get direct filter if filter is not a parameter', () => {
            expect(selectOnlyMineFilter({ ...store, 'tools-access-react-redux-router': '/' }))
                .toEqual(OnlyMineFilter.DIRECT);
        });

        it('Should select the last filter parameter', () => {
            expect(selectOnlyMineFilter({ ...store, 'tools-access-react-redux-router': '/?only_mine=direct&only_mine=hierarchy' }))
                .toEqual(OnlyMineFilter.HIERARCHY);
        });

        it('Should get direct filter if filter is not a filter value', () => {
            expect(selectOnlyMineFilter({ ...store, 'tools-access-react-redux-router': '/?only_mine=2jfjf' }))
                .toEqual(OnlyMineFilter.DIRECT);
        });
    });

    describe('selectRequests', () => {
        it('Should select requests', () => {
            const myRequests: ApproveRequest[] = [getRequest(100), getRequest(200)];

            expect(selectRequests({
                ...store,
                serviceCreation: {
                    ...store.serviceCreation,
                    myRequests,
                },
            }))
                .toEqual(myRequests);
        });
    });

    describe('selectRequestsTotalPages', () => {
        it('Should total pages', () => {
            const myRequestsTotalPages = 12;

            expect(selectRequestsTotalPages({
                ...store,
                serviceCreation: {
                    ...store.serviceCreation,
                    myRequestsTotalPages,
                },
            }))
                .toEqual(myRequestsTotalPages);
        });
    });
});
