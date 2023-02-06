import { createAction } from 'redux-actions';
import { dispatchSerial } from 'tools-access-react-redux/src/serial';

import { requestJson } from './request-json';
import {
    DUTY_ABSENCES_UPDATE,
    DUTY_ABSENCES_SET_FILTERS,
    DUTY_ABSENCES_SET_LOADING,
    updateDutyAbsences,
} from './duty-absences';
import handleActions from './duty-absences';

describe('dutyAbsences', () => {
    it('Should create JSON_REQUEST action', () => {
        const filters = {
            serviceId: 2,
            dateFrom: new Date(2019, 1, 18),
            dateTo: new Date(2019, 4, 18),
        };
        const result = dispatchSerial([
            createAction(DUTY_ABSENCES_SET_FILTERS)(filters),
            createAction(DUTY_ABSENCES_SET_LOADING)(true),
            requestJson({
                pathname: '/back-proxy/api/v3/duty/gaps/',
                query: {
                    service: 2,
                    date_from: '2019-02-18',
                    date_to: '2019-05-18',
                    page: 1,
                    page_size: 1000,
                },
            }, DUTY_ABSENCES_UPDATE),
            createAction(DUTY_ABSENCES_SET_LOADING)(false),
        ]);

        expect(updateDutyAbsences(filters))
            .toEqual(result);
    });

    it('Should handle DUTY_ABSENCES_SET_LOADING action', () => {
        const result = { loading: true };

        expect(handleActions({}, {
            type: DUTY_ABSENCES_SET_LOADING,
            payload: true,
        })).toEqual(result);
    });

    it('Should handle DUTY_ABSENCES_SET_FILTERS action', () => {
        const filters = {
            serviceId: 2,
            dateFrom: '2019-02-18',
            dateTo: '2019-05-18',
        };
        const result = { filters };

        expect(handleActions({}, {
            type: DUTY_ABSENCES_SET_FILTERS,
            payload: filters,
        })).toEqual(result);
    });

    it('Should handle DUTY_ABSENCES_UPDATE action', () => {
        const result = {
            error: null,
            data: [{
                id: 1,
                start: expect.toMatchDate(new Date(2000, 0, 1)),
                end: expect.toMatchDate(new Date(2000, 0, 10)),
                type: 'type',
                person: 'person',
                workInAbsence: true,
                fullDay: true,
            }, {
                id: 2,
                start: expect.toMatchDate(new Date(Date.UTC(2000, 0, 1, 12, 45, 1))),
                end: expect.toMatchDate(new Date(Date.UTC(2000, 0, 1, 13))),
                type: 'type',
                person: 'person',
                workInAbsence: false,
                fullDay: false,
            }],
        };

        expect(handleActions({}, {
            type: DUTY_ABSENCES_UPDATE,
            payload: {
                results: [{
                    id: 1,
                    start: '2000-01-01',
                    end: '2000-01-10',
                    type: 'type',
                    person: 'person',
                    work_in_absence: true,
                    full_day: true,
                }, {
                    id: 2,
                    start: '2000-01-01T12:45:01Z',
                    end: '2000-01-01T13:00:00Z',
                    type: 'type',
                    person: 'person',
                    work_in_absence: false,
                    full_day: false,
                }],
            },
            error: false,
        })).toEqual(result);
    });

    it('Should handle DUTY_ABSENCES_UPDATE error', () => {
        const result = {
            error: { message: 'error' },
            data: [],
        };

        expect(handleActions({}, {
            type: DUTY_ABSENCES_UPDATE,
            payload: { message: 'error' },
            error: true,
        })).toEqual(result);
    });
});
