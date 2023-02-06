import React from 'react';
import { render } from 'enzyme';
import { DateTime } from 'luxon';

import { Time, format } from './Time';

describe('Time formatting', () => {
    it('Should accept expected value data types', () => {
        expect(format({
            value: new Date(Date.UTC(2000, 0, 1, 0, 0, 0)),
            zone: 'UTC',
        }))
            .toBe('12:00 AM UTC');
        expect(format({
            value: DateTime.fromJSDate(new Date(Date.UTC(2000, 0, 1, 0, 0, 0))),
            zone: 'UTC',
        }))
            .toBe('12:00 AM UTC');
    });

    it('Should omit seconds', () => {
        expect(format({
            value: new Date(Date.UTC(2000, 0, 1, 0, 0, 22)),
            zone: 'UTC',
        })).toBe('12:00 AM UTC');
    });

    describe('Should specify offset when displaying time in a non-local timezone', () => {
        it('Should use short offset name by default', () => {
            expect(format({
                value: new Date(Date.UTC(2000, 0, 1, 12, 34)),
                zone: 'America/Detroit',
            })).toBe('7:34 AM EST');
        });

        it('Should use passed offset name', () => {
            expect(format({
                value: new Date(Date.UTC(2000, 0, 1, 12, 34)),
                zone: 'America/Detroit',
                zoneOffsetName: 'HAHA',
            })).toBe('7:34 AM HAHA');
        });
    });
});

describe('Time component', () => {
    it('Should render Time', () => {
        const wrapper = render(
            <Time
                value={new Date(Date.UTC(2000, 0, 1, 12, 34, 56))}
                zone="Europe/Moscow"
                zoneOffsetName="МСК"
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
