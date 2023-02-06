import { getGapInfo, getWorkflowI18n } from '../userGap';
import * as ru from '../../../langs/yamb/ru.json';
import i18n from '../../../shared/lib/i18n';
import { Workflows } from '../../constants/yamb';

describe('userGap helper', () => {
    i18n.locale('ru', ru);
    global.Date.now = jest.fn(() => 1581800400000);
    describe('#getGapInfo', () => {
        it('Should return gap info for today absence', () => {
            const data = {
                date_from: '2020-02-16T00:00:00',
                date_to: '2020-02-17T00:00:00',
                full_day: true,
                workflow: 'absence',
            } as any as APIv3.UserGap;

            expect(getGapInfo(data)).toEqual(i18n('chat.gap.absence.today_full'));
        });

        it('Should return valid workflow name in user gap', () => {
            const workflows: APIv3.WorkflowType[] = [
                'absence', 'trip', 'conference_trip', 'conference',
                'learning', 'vacation', 'paid_day_off', 'illness', 'maternity',
            ];
            const data = {
                date_from: '2020-02-16T00:00:00',
                date_to: '2020-02-17T00:00:00',
                full_day: true,
            } as any as APIv3.UserGap;

            for (const workflow of workflows) {
                data.workflow = workflow;
                expect(getGapInfo(data)).toEqual(i18n(getWorkflowI18n(workflow).full));
            }
        });

        it('Should return absence for unknown workflow', () => {
            const data = {
                date_from: '2020-02-16T00:00:00',
                date_to: '2020-02-17T00:00:00',
                full_day: true,
                workflow: 'some_unknown_workflow',
            } as any as APIv3.UserGap;

            expect(getGapInfo(data)).toEqual(i18n(getWorkflowI18n(Workflows.ABSENCE).full));
        });

        it('Should return absence for undefined workflow', () => {
            const data = {
                date_from: '2020-02-16T00:00:00',
                date_to: '2020-02-17T00:00:00',
                full_day: true,
                workflow: undefined,
            } as any as APIv3.UserGap;

            expect(getGapInfo(data)).toEqual(i18n(getWorkflowI18n(Workflows.ABSENCE).full));
        });

        it('Should return valid time period in user gap', () => {
            const data = {
                date_from: '2020-02-15T21:00:00',
                date_to: '2020-02-16T19:30:00',
                full_day: false,
                workflow: 'absence',
            } as any as APIv3.UserGap;

            expect(getGapInfo(data)).toEqual(i18n('chat.gap.absence.today', {
                from: '0:00',
                to: '22:30',
            }));
        });

        it('Should return valid date period in user gap', () => {
            const data = {
                date_from: '2020-02-16T00:00:00',
                date_to: '2020-02-19T00:00:00',
                full_day: true,
                workflow: 'absence',
            } as any as APIv3.UserGap;

            expect(getGapInfo(data)).toEqual(i18n('chat.gap.absence.date', {
                from: '16 февраля',
                to: '18 февраля',
            }));
        });

        it('Should return valid date and time period in user gap', () => {
            const data = {
                date_from: '2020-02-16T21:00:00',
                date_to: '2020-02-19T19:30:00',
                full_day: false,
                workflow: 'absence',
            } as any as APIv3.UserGap;

            expect(getGapInfo(data)).toEqual(i18n('chat.gap.absence.date', {
                from: '17 февраля 0:00',
                to: '19 февраля 22:30',
            }));
        });
    });
});
