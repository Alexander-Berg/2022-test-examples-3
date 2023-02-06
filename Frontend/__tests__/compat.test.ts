import { transformRightsToNumber } from '../compat';
import { UserRights } from '../../constants/Chat';

import {
    deserializePlainMessage,
    normalizeScheduleItem,
} from '../compat';

function timeMs(hours, minutes, timezoneOffset) {
    return (hours * 60 + minutes - timezoneOffset) * 60 * 1000;
}

describe('Compat', () => {
    describe('#deserializePlainMessage', () => {
        it('returns plain with replay and mentions', () => {
            const fromServer: Fanout.ServerMessageWrapper = require('./compat/source/plain-with-reply-mentions');
            const clientMessage: APIv3.Message = require('./compat/expectations/plain-with-reply-mentions');

            expect(deserializePlainMessage(fromServer.ServerMessage)).toMatchObject(clientMessage);
        });

        it('returns plain with gallery empty items', () => {
            const fromServer: Fanout.ServerMessageWrapper = require('./compat/source/plain-gallery-empty-items');
            const clientMessage: APIv3.Message = require('./compat/expectations/plain-gallery-empty-items');

            expect(deserializePlainMessage(fromServer.ServerMessage)).toMatchObject(clientMessage);
        });

        it('returns plain with gallery items', () => {
            const fromServer: Fanout.ServerMessageWrapper = require('./compat/source/plain-gallery-items');
            const clientMessage: APIv3.Message = require('./compat/expectations/plain-gallery-items');

            expect(deserializePlainMessage(fromServer.ServerMessage)).toMatchObject(clientMessage);
        });
    });

    describe('#normalizeScheduleItem', () => {
        it('returns intervals with number of seconds', () => {
            const timezoneOffset = 0;
            const scheduleItem = { from_hour: '12:00', to_hour: '16:00', weekday: 1 };

            expect(normalizeScheduleItem(scheduleItem, timezoneOffset)).toEqual({
                from: timeMs(12, 0, timezoneOffset),
                to: timeMs(16, 0, timezoneOffset),
                weekday: 1,
            });
        });

        it('returns \'to\' plus a day number of seconds when to_hour < from_hour', () => {
            const timezoneOffset = 0;
            const scheduleItem = { from_hour: '12:15', to_hour: '01:15', weekday: 2 };

            expect(normalizeScheduleItem(scheduleItem, timezoneOffset)).toEqual({
                from: timeMs(12, 15, timezoneOffset),
                to: timeMs(1, 15, timezoneOffset) + timeMs(24, 0, 0),
                weekday: 2,
            });
        });

        it('returns \'to\' plus a day number of seconds when to_hour = from_hour', () => {
            const timezoneOffset = 0;
            const scheduleItem = { from_hour: '00:00', to_hour: '00:00', weekday: 3 };

            expect(normalizeScheduleItem(scheduleItem, timezoneOffset)).toEqual({
                from: timeMs(0, 0, timezoneOffset),
                to: timeMs(0, 0, timezoneOffset) + timeMs(24, 0, 0),
                weekday: 3,
            });
        });

        it('returns expected intervals if to_hour > from_hour', () => {
            const timezoneOffset = 0;
            const scheduleItem = { from_hour: '16:00', to_hour: '16:01', weekday: 4 };

            expect(normalizeScheduleItem(scheduleItem, timezoneOffset)).toEqual({
                from: timeMs(16, 0, timezoneOffset),
                to: timeMs(16, 1, timezoneOffset),
                weekday: 4,
            });
        });

        it('returns intervals calculated to UTC', () => {
            const timezoneOffset = 7 * 60;
            const scheduleItem = { from_hour: '7:00', to_hour: '14:00', weekday: 5 };

            expect(normalizeScheduleItem(scheduleItem, timezoneOffset)).toEqual({
                from: timeMs(7, 0, timezoneOffset),
                to: timeMs(14, 0, timezoneOffset),
                weekday: 5,
            });
        });

        it('returns previous weekday and intervals minus a day when from_hour calculated to UTC is negative', () => {
            const timezoneOffset = 2 * 60;
            const scheduleItem = { from_hour: '01:00', to_hour: '06:00', weekday: 0 };

            expect(normalizeScheduleItem(scheduleItem, timezoneOffset)).toEqual({
                from: timeMs(1, 0, timezoneOffset) + timeMs(24, 0, 0),
                to: timeMs(6, 0, timezoneOffset) + timeMs(24, 0, 0),
                weekday: 6,
            });
        });

        it('returns sunday weekday as 0 when input weekday is 7', () => {
            const timezoneOffset = 0;
            const scheduleItem = { from_hour: '12:00', to_hour: '16:00', weekday: 7 };

            expect(normalizeScheduleItem(scheduleItem, timezoneOffset)).toEqual({
                from: timeMs(12, 0, timezoneOffset),
                to: timeMs(16, 0, timezoneOffset),
                weekday: 0,
            });
        });
    });

    describe('#transformRightsToNumber', () => {
        it('Should return 5', () => {
            expect(transformRightsToNumber([UserRights.JOIN, UserRights.READ])).toBe(5);
        });
        it('Should return 14', () => {
            expect(transformRightsToNumber([UserRights.LEAVE, UserRights.READ, UserRights.WRITE])).toBe(14);
        });
    });
});
