import { getStatusType } from '.';

describe('Status.util', function() {
    describe('getStatusType', function() {
        it('normal -> label', function() {
            expect(getStatusType('normal')).toEqual('label');
        });
        it('bullet -> badge', function() {
            expect(getStatusType('normal')).toEqual('label');
        });
    });
});
