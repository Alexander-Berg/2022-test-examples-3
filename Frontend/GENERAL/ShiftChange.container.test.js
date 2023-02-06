import { mapStateToProps } from './ShiftChange.container';

describe('ShiftChangeConnected', () => {
    describe('mapStateToProps', () => {
        it('find schedule', () => {
            const schedules = [{ id: 100, startTime: '12:00' }];
            const state = { dutySchedules: { collection: schedules } };

            expect(mapStateToProps(state, { scheduleId: 100 })).toEqual({ time: '12:00' });
        });

        it('does not find schedule', () => {
            const schedules = [{ id: 200, startTime: '12:00' }];
            const state = { dutySchedules: { collection: schedules } };

            expect(mapStateToProps(state, { scheduleId: 100 })).toEqual({});
        });

        it('no scheduleId', () => {
            const schedules = [{ id: 200, startTime: '12:00' }, { startTime: '20:00' }];
            const state = { dutySchedules: { collection: schedules } };

            expect(mapStateToProps(state, { scheduleId: undefined })).toEqual({});
        });
    });
});
