import { ICandidate } from '~/interfaces/ICandidate';
import { CONSIDERATION_STATUS } from '~/enums/ConsiderationStatus';
import { IConsideration } from '~/interfaces/IConsideration';
import { IApplication } from '~/interfaces/IApplication';
import { IDataRequestResults } from '~/interfaces/IDataRequestResults';
import { ISForm } from '~/interfaces/ISForm';

let index = 0;
function getUniqId() {
    return index++;
}

export const getFormMock = (partialData?: Partial<ISForm<null>>): ISForm<null> => ({
    structure: {
        vacancies: {
            name: 'vacancies',
            required: true,
            value: [],
            type: 'multiplesuggest',
            key: 'vacancies',
            types: [
                'vacancy',
            ],
        },
        stage: {
            name: 'stage',
            required: true,
            value: '',
            type: 'choice',
            key: 'stage',
            choices: [
                {
                    value: 'draft',
                    label: 'draft',
                },
                {
                    value: 'new',
                    label: 'new',
                },
                {
                    value: 'team_is_interested',
                    label: 'team_is_interested',
                },
                {
                    value: 'invited_to_preliminary_interview',
                    label: 'invited_to_preliminary_interview',
                },
                {
                    value: 'invited_to_onsite_interview',
                    label: 'invited_to_onsite_interview',
                },
                {
                    value: 'invited_to_final_interview',
                    label: 'invited_to_final_interview',
                },
                {
                    value: 'offer_agreement',
                    label: 'offer_agreement',
                },
                {
                    value: 'closed',
                    label: 'closed',
                },
                {
                    value: 'did_not_pass_assessments',
                    label: 'application.resolution.did_not_pass_assessments',
                },
                {
                    value: 'team_was_not_interested',
                    label: 'application.resolution.team_was_not_interested',
                },
                {
                    value: 'team_was_not_selected',
                    label: 'application.resolution.team_was_not_selected',
                },
                {
                    value: 'refused_us',
                    label: 'application.resolution.refused_us',
                },
                {
                    value: 'offer_rejected',
                    label: 'application.resolution.offer_rejected',
                },
                {
                    value: 'offer_accepted',
                    label: 'application.resolution.offer_accepted',
                },
                {
                    value: 'rotated',
                    label: 'application.resolution.rotated',
                },
                {
                    value: 'vacancy_closed',
                    label: 'application.resolution.vacancy_closed',
                },
                {
                    value: 'incorrect',
                    label: 'application.resolution.incorrect',
                },
                {
                    value: 'on_hold',
                    label: 'application.resolution.on_hold',
                },
                {
                    value: 'consideration_archived',
                    label: 'application.resolution.consideration_archived',
                },
            ],
        },
    },
    data: undefined,
    meta: null,
    ...partialData,
});

export interface LabelField {
    id: number;
    data: {
        status: string;
        name: string;
        caption: string;
    }
}

export const getMockSFormData = (fields: LabelField[]): ISForm<null>['data'] => {
    return {
        vacancies: {
            name: 'vacancies',
            required: true,
            value: fields.map(field => field.id),
            label_fields: fields.reduce((acc: Record<string, LabelField['data']>, field: LabelField) => {
                acc[field.id] = field.data;

                return acc;
            }, {}),
        },
        stage: {
            name: 'stage',
            required: true,
            value: '',
        },
    }
}

export const getConsiderationStub = (partialData?: Partial<IConsideration>): IConsideration => {
    return {
        id: getUniqId(),
        status: 'in_progress',
        resolution: '',
        extended_status: CONSIDERATION_STATUS.IN_PROGRESS,
        interviews: [],
        source: '',
        source_description: '',
        finished: undefined,
        counts: {
            applications: 0,
            assessments: 0,
            messages: 0
        },
        responsibles: [],
        started: new Date(),
        ...partialData
    }
}

export const getCandidateStub = (partialData?: Partial<ICandidate>): ICandidate => {
    return {
        id: getUniqId(),
        last_name: 'LastName',
        first_name: 'FirstName',
        extended_status: 'in_progress',
        skills: [],
        target_cities: [],
        status: 'in_progress',
        applications: undefined,
        city: '',
        country: '',
        birthday: undefined,
        gender: '',
        is_current_employee: false,
        login: '',
        middle_name: '',
        modified: new Date(),
        professions: [],
        vacancies: undefined,
        ...partialData,
    }
}

export const getApplicationStub = (partialData?: Partial<IApplication>): IApplication => {
    return {
        id: getUniqId(),
        candidate: getCandidateStub(),
        consideration: getConsiderationStub(),
        ...partialData
    }
}

export const getRequestStub = (partialData?: Partial<IDataRequestResults<IApplication>>): IDataRequestResults<IApplication> => {
    return {
        count: partialData?.results?.length || 0,
        next: '',
        previous: null,
        results: [],
        ...partialData
    }
};
