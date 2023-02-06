import { STATUS_ENTITY } from '../Status.types';

import { getStatusText } from '.';

describe('Status.util', function() {
    describe('getStatusText', function() {
        it('should return correct text', function() {
            expect(getStatusText(STATUS_ENTITY.CANDIDATE_EXTENDED, 'in-progress')).toEqual('В работе');
        });
    });
});
