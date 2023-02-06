import { selectShift } from './DutyShifts.selectors';

describe('DutyShifts.selectors', () => {
    describe('selectShift', () => {
        it('Should select shift by id', () => {
            expect(selectShift({
                dutyShifts: {
                    southpark: {
                        data: [{
                            id: 42,
                            foo: 'bar',
                        }],
                    },
                },
            }, 'southpark', 42)).toEqual({
                id: 42,
                foo: 'bar',
            });
        });
    });
});
