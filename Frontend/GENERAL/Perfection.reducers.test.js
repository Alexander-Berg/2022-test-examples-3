import handleActions from './Perfection.reducers';
import {
    COMPLAINTS_RESET_ERROR,
    COMPLAINTS_SET_LOADING,
    ISSUE_GROUPS_RESET_ERROR,
    ISSUE_GROUPS_SET_LOADING,
    ISSUE_GROUPS_UPDATE,
    UPDATE_COMPLAINTS,
    POST_APPEAL,
    APPEAL_SET_LOADING,
    APPEAL_ERROR_AND_SUCCESS_RESET,
    UPDATE_APPEAL,
    LEVEL_CRITICAL,
    LEVEL_WARNING,
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
                    should_be_applied_at: '2018-08-17T00:15:16Z',
                    is_critical: true,
                    applied_at: null,
                },
                on_review: false,
                can_be_appealed: false,
                weight: '20',
                is_appealed: false,
            },
        ],
        description: { ru: 'Сервис находится в желтой зоне по количеству проблем в группе Команда' },
        recommendation: { ru: 'Исправьте проблемы в сервисе и не допускайте новыx' },
        summary: {
            current_weight: 45,
            thresholds: [
                {
                    level: LEVEL_CRITICAL,
                    is_next: true,
                    is_current: false,
                    execution: {
                        name: { ru: 'Сервис перейдет в красную зону при достижении порога 33% и будет Закрыт' },
                        should_be_applied_at: '2020-10-17T00:15:16Z',
                        is_critical: true,
                    },
                    weight: 60,
                },
                {
                    level: LEVEL_WARNING,
                    is_next: false,
                    is_current: true,
                    weight: 30,
                    execution: {
                        name: { ru: 'Сервис перейдёт в статус Требуется информация' },
                        should_be_applied_at: '2020-10-17T00:15:16Z',
                        is_critical: true,
                        applied_at: null,
                    },
                },
            ],
        },
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
        description: { ru: 'Сервис находится в красной зоне по количеству проблем в группе Команда' },
        recommendation: { ru: 'Исправьте проблемы в сервисе и не допускайте новыx' },
        summary: {
            current_weight: 78,
            thresholds: [
                {
                    level: LEVEL_CRITICAL,
                    is_next: false,
                    is_current: true,
                    execution: {
                        name: { ru: 'Сервис перейдёт в статус Требуется информация' },
                        should_be_applied_at: '2020-10-17T00:15:16Z',
                        is_critical: true,
                    },
                    weight: 60,
                },
            ],
        },
    }],
};

const parsedIssueGroupPayload = [{
    name: { ru: 'Понятность', en: 'Understandability' },
    issuesCount: 1,
    issues: [
        {
            id: 303,
            name: { ru: 'Отсутствие руководителя', en: 'Lack of head' },
            description: { ru: '', en: '' },
            recommendation: { ru: '', en: '' },
            execution: null,
            onReview: false,
            canBeAppealed: false,
            weight: 10,
            isAppealed: false,
        },
        {
            id: 1834,
            name: { ru: 'Отсутствует описание', en: 'Description is empty' },
            description: { ru: '', en: '' },
            recommendation: { ru: '', en: '' },
            execution: {
                name: { ru: '', en: '' },
                shouldBeAppliedAt: new Date('2018-08-17T00:15:16Z'),
                isCritical: true,
                appliedAt: null,
            },
            onReview: false,
            canBeAppealed: false,
            weight: 20,
            isAppealed: false,
        },
    ],
    description: { ru: 'Сервис находится в желтой зоне по количеству проблем в группе Команда' },
    recommendation: { ru: 'Исправьте проблемы в сервисе и не допускайте новыx' },
    summary: {
        currentWeight: 55,
        currentLevel: 'warning',
        zones: [
            { level: 'critical', width: 40 },
            { level: 'warning', width: 30 },
            { level: 'ok', width: 30 },
        ],
        currentExecution: {
            name: { ru: 'Сервис перейдёт в статус Требуется информация' },
            shouldBeAppliedAt: new Date('2020-10-17T00:15:16Z'),
            isCritical: true,
            appliedAt: null,
        },
        nextExecution: {
            name: { ru: 'Сервис перейдет в красную зону при достижении порога 33% и будет Закрыт' },
            shouldBeAppliedAt: new Date('2020-10-17T00:15:16Z'),
            isCritical: true,
        },
    },
}, {
    name: { ru: 'Вложенные сервисы', en: 'Children' },
    issuesCount: 1,
    issues: [
        {
            id: 4990,
            name: { ru: 'Больше 30% вложенных сервисов красные', en: 'More than 30% of descendants are red' },
            description: { ru: '', en: '' },
            recommendation: { ru: '', en: '' },
            execution: null,
            onReview: false,
            canBeAppealed: false,
            weight: 30,
            isAppealed: false,
        },
    ],
    description: { ru: 'Сервис находится в красной зоне по количеству проблем в группе Команда' },
    recommendation: { ru: 'Исправьте проблемы в сервисе и не допускайте новыx' },
    summary: {
        currentWeight: 22,
        currentLevel: 'critical',
        zones: [
            { level: 'critical', width: 40 },
            { level: 'ok', width: 60 },
        ],
        currentExecution: {
            name: { ru: 'Сервис перейдёт в статус Требуется информация' },
            shouldBeAppliedAt: new Date('2020-10-17T00:15:16Z'),
            isCritical: true,
        },
        nextExecution: null,
    },
}];

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

const parsedComplaints = {
    count: 4,
    totalPages: 2,
    results: [
        {
            createdAt: new Date('2019-09-13T14:41:06.013147Z'),
            message: 'Всё плохо',
        },
        {
            createdAt: new Date('2019-09-14T14:41:06.013147Z'),
            message: 'Всё почти плохо',
        },
    ],
};

describe('Should handle actions', () => {
    it('ISSUE_GROUPS_UPDATE', () => {
        expect(handleActions({
            issueGroupsError: 'error',
            issueGroups: [],
        }, {
            type: ISSUE_GROUPS_UPDATE,
            payload: rawIssueGroupPayload,
            error: false,
        })).toEqual({
            issueGroupsError: null,
            issueGroups: parsedIssueGroupPayload,
        });

        expect(handleActions({
            issueGroups: [],
            issueGroupsError: null,
        }, {
            type: ISSUE_GROUPS_UPDATE,
            payload: 'error',
            error: true,
        })).toEqual({
            issueGroupsError: 'error',
            issueGroups: [],
        });
    });

    it('ISSUE_GROUPS_SET_LOADING', () => {
        expect(handleActions({
            issueGroupsLoading: false,
        }, {
            type: ISSUE_GROUPS_SET_LOADING,
            payload: true,
        })).toEqual({
            issueGroupsLoading: true,
        });

        expect(handleActions({
            issueGroupsLoading: true,
        }, {
            type: ISSUE_GROUPS_SET_LOADING,
            payload: false,
        })).toEqual({
            issueGroupsLoading: false,
        });
    });

    it('ISSUE_GROUPS_RESET_ERROR', () => {
        expect(handleActions({
            issueGroupsError: 'error',
            issueGroups: [],
        }, {
            type: ISSUE_GROUPS_RESET_ERROR,
        })).toEqual({
            issueGroupsError: null,
            issueGroups: [],
        });
    });

    it('UPDATE_COMPLAINTS', () => {
        expect(handleActions({
            complaintsError: 'error',
            complaints: [],
        }, {
            type: UPDATE_COMPLAINTS,
            payload: rawComplaintsPayload,
            error: false,
        })).toEqual({
            complaintsError: null,
            complaints: parsedComplaints,
        });

        expect(handleActions({
            complaints: [],
            complaintsError: null,
        }, {
            type: UPDATE_COMPLAINTS,
            payload: 'error',
            error: true,
        })).toEqual({
            complaintsError: 'error',
            complaints: [],
        });
    });

    it('COMPLAINTS_SET_LOADING', () => {
        expect(handleActions({
            complaintsLoading: false,
        }, {
            type: COMPLAINTS_SET_LOADING,
            payload: true,
        })).toEqual({
            complaintsLoading: true,
        });

        expect(handleActions({
            complaintsLoading: true,
        }, {
            type: COMPLAINTS_SET_LOADING,
            payload: false,
        })).toEqual({
            complaintsLoading: false,
        });
    });

    it('COMPLAINTS_RESET_ERROR', () => {
        expect(handleActions({
            complaintsError: 'error',
            complaints: parsedComplaints,
        }, {
            type: COMPLAINTS_RESET_ERROR,
        })).toEqual({
            complaintsError: null,
            complaints: parsedComplaints,
        });
    });

    it('UPDATE_APPEAL', () => {
        expect(handleActions({
            appealError: { '100': 'error' },
            appealSuccess: { '100': false },
        }, {
            type: UPDATE_APPEAL,
            payload: { payload: 'text', id: 100 },
            error: false,
        })).toEqual({
            appealError: { '100': null },
            appealSuccess: { '100': true },
        });

        expect(handleActions({
            appealError: {},
        }, {
            type: UPDATE_APPEAL,
            payload: { payload: 'error', id: 100 },
            error: true,
            id: 100,
        })).toEqual({
            appealError: { '100': 'error' },
            appealSuccess: { '100': false },
        });
    });

    it('APPEAL_SET_LOADING', () => {
        expect(handleActions({}, {
            type: APPEAL_SET_LOADING,
            payload: { payload: true, id: 100 },
        })).toEqual({
            appealLoading: { '100': true },
        });

        expect(handleActions({
            appealLoading: { '100': true },
        }, {
            type: APPEAL_SET_LOADING,
            payload: { payload: false, id: 100 },
        })).toEqual({
            appealLoading: { '100': false },
        });
    });

    it('APPEAL_ERROR_AND_SUCCESS_RESET', () => {
        expect(handleActions({
            appealError: { '100': 'error' },
            appealSuccess: { '100': true },
        }, {
            type: APPEAL_ERROR_AND_SUCCESS_RESET,
            payload: { id: 100 },
        })).toEqual({
            appealError: { '100': null },
            appealSuccess: { '100': false },
        });
    });

    it('POST_APPEAL', () => {
        expect(handleActions({
            appealError: { '100': null },
            appealSuccess: { '100': false },
        }, {
            type: POST_APPEAL,
        })).toEqual({
            appealError: { '100': null },
            appealSuccess: { '100': false },
        });
    });
});
