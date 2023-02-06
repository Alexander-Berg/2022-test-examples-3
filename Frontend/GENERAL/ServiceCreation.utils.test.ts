import { getRequest } from '../components/Requests/testData/testData';
import { getRequestsWithChangedRequest } from './ServiceCreation.utils';

describe('utils', () => {
    it('Should update request', () => {
        const requests = [getRequest(100), getRequest(200), getRequest(300)];

        const expectedResult = [{ ...getRequest(100), isApproved: true }, getRequest(200), getRequest(300)];
        const actualResult = getRequestsWithChangedRequest(requests, 100, { isApproved: true });

        expect(actualResult).toEqual(expectedResult);
    });

    it('Should update nothing because request id is not in requests', () => {
        const requests = [getRequest(100), getRequest(200), getRequest(300)];

        const actualResult = getRequestsWithChangedRequest(requests, 1, { isApproved: true });

        expect(actualResult).toEqual(requests);
    });

    it('Should return undefined if input is undefined', () => {
        const requests = undefined;

        const expectedResult = undefined;
        const actualResult = getRequestsWithChangedRequest(requests, 100, { isApproved: true });

        expect(actualResult).toEqual(expectedResult);
    });
});
