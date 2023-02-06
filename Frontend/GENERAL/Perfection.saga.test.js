import { take, call, put, all } from 'redux-saga/effects';
import { request } from '../../../abc/react/redux/request-json';
import {
    watchIssueGroups,
    watchComplaints,
    watchAppeal,
} from './Perfection.saga';

import {
    ISSUE_GROUPS_REQUEST,
    COMPLAINTS_REQUEST,
    POST_APPEAL,
} from './Perfection.actions';

import {
    issueGroupRequest,
    setIssueGroupLoading,
    updateIssueGroups,
    updateComplaints,
    complaintsRequest,
    setComplaintsLoading,
    postAppeal,
    setAppealLoading,
    updateAppeal,
} from './Perfection.actions';

const rawIssueGroupPayload = {
    issues: [{
        name: { ru: 'Понятность', en: 'Understandability' },
        issues_count: 1,
        issues: [
            {
                id: 303,
                name: { ru: 'Отсутствие руководителя', en: 'Lack of head' },
                description: { ru: '', en: '' },
                recommendation: { ru: '', en: '' },
                execution: null,
                on_review: false,
                can_be_appealed: false,
                weight: '10',
                is_appealed: false,
            },
            {
                id: 1834,
                name: { ru: 'Отсутствует описание', en: 'Description is empty' },
                description: { ru: '', en: '' },
                recommendation: { ru: '', en: '' },
                execution: {
                    name: { ru: '', en: '' },
                    apply_date: '2018-08-17T00:15:16Z',
                    is_critical: true,
                },
                on_review: false,
                can_be_appealed: false,
                weight: '20',
                is_appealed: false,
            },
        ],
    }, {
        name: { ru: 'Вложенные сервисы', en: 'Children' },
        issues_count: 1,
        issues: [
            {
                id: 4990,
                name: { ru: 'Больше 30% вложенных сервисов красные', en: 'More than 30% of descendants are red' },
                description: { ru: '', en: '' },
                recommendation: { ru: '', en: '' },
                execution: null,
                on_review: false,
                can_be_appealed: false,
                weight: '30',
                is_appealed: false,
            },
        ],
    }],
};
const rawComplaintsPayload = {
    complaints: {
        count: 4,
        total_pages: 2,
        results: [
            {
                created_at: '2019-09-13T14:41:06.013147Z',
                message: 'Всё плохо',
                service: 2905,
            },
            {
                created_at: '2019-09-14T14:41:06.013147Z',
                message: 'Всё почти плохо',
                service: 2905,
            },
        ],
    },
};

describe('Perfection Saga', () => {
    describe('* watchIssueGroups', () => {
        it('Should generate effects', () => {
            const gen = watchIssueGroups();

            expect(gen.next().value)
                .toEqual(take(ISSUE_GROUPS_REQUEST));

            expect(gen.next(issueGroupRequest({ serviceId: 5 })).value)
                .toEqual(put(setIssueGroupLoading(true)));

            expect(gen.next().value)
                .toEqual(all({
                    issues: call(request, {
                        pathname: '/back-proxy/api/frontend/services/5/issuegroups/',
                    }),
                }));

            expect(gen.next(rawIssueGroupPayload).value)
                .toEqual(put(updateIssueGroups({
                    issues: rawIssueGroupPayload.issues,
                    error: null,
                })));

            expect(gen.next({}).value)
                .toEqual(put(setIssueGroupLoading(false)));
        });
    });

    describe('* watchComplaints', () => {
        it('Should generate effects', () => {
            const gen = watchComplaints();

            expect(gen.next().value)
                .toEqual(take(COMPLAINTS_REQUEST));

            expect(gen.next(complaintsRequest({ serviceId: 5, page: 1, pageSize: 2 })).value)
                .toEqual(put(setComplaintsLoading(true)));

            expect(gen.next().value)
                .toEqual(all({
                    complaints: call(request, {
                        pathname: '/back-proxy/api/v3/suspicion/complaints/',
                        query: {
                            service: 5,
                            page: 1,
                            page_size: 2,
                        },
                    }),
                }));

            expect(gen.next(rawComplaintsPayload).value)
                .toEqual(put(updateComplaints({
                    complaints: rawComplaintsPayload.complaints,
                    error: null,
                })));

            expect(gen.next({}).value)
                .toEqual(put(setComplaintsLoading(false)));
        });
    });

    describe('* watchAppeal', () => {
        it('Should generate effects', () => {
            const gen = watchAppeal();

            expect(gen.next().value)
                .toEqual(take(POST_APPEAL));

            expect(gen.next(postAppeal({ issue: 1001, message: 'Всё нормально' })).value)
                .toEqual(put(setAppealLoading({ id: 1001, payload: true })));

            expect(gen.next().value)
                .toEqual(all({
                    appeal: call(request, {
                        method: 'POST',
                        pathname: '/back-proxy/api/v3/suspicion/appeals/',
                        data: {
                            issue: 1001,
                            message: 'Всё нормально',
                        },
                    }),
                }));

            expect(gen.next().value)
                .toEqual(put(updateAppeal({
                    payload: { error: null },
                    id: 1001,
                })));

            expect(gen.next({}).value)
                .toEqual(put(setAppealLoading({ id: 1001, payload: false })));
        });
    });
});
