import { selectSchedule, selectShiftsWithSchedules } from './DutySchedules.selectors';
import { state, shiftsWithScheduleFields, schedule200 } from './testData';

describe('DutySchedules selectors', () => {
    it('selectSchedule', () => {
        const schedule = selectSchedule(state, 200);

        expect(schedule).toEqual(schedule200);
    });

    it('selectShiftsWithSchedules', () => {
        const shifts = selectShiftsWithSchedules(state, 'calendar', ['id', 'name']);

        expect(shifts).toEqual(shiftsWithScheduleFields);
    });
});
