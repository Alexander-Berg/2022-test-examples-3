import nock, { Scope } from 'nock';
import Mock from '../../types/mock';

/**
 * Raise nock by mocks array
 * @param {Array<Object>} mocks
 * @param {number} [mocks.times = 1] count of repeating times
 * @returns {Array<Scope>}
 */
function raiseMocks(...mocks: Array<Mock>): Array<Scope> {
    const mockScopes: any[] = [];

    mocks.forEach((mock) => {
        const scope: Scope = nock(mock.host, {
            allowUnmocked: mock.allowUnmocked === undefined ? true : mock.allowUnmocked,
        });
        scope[mock.method || 'get'](mock.route)
            .times(mock.times || 1)
            .reply(200, mock.response);

        mockScopes.push(scope);
    });

    return mockScopes;
}

export default raiseMocks;
