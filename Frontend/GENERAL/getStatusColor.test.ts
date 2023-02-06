import { STATUS_ENTITY } from '../Status.types';

import { getStatusColor } from '.';

describe('Status.util', function() {
    describe('getStatusColor', function() {
        it('should return correct color', function() {
            expect(getStatusColor(STATUS_ENTITY.CANDIDATE_EXTENDED, 'in-progress')).toEqual('green');
        });
    });
});
