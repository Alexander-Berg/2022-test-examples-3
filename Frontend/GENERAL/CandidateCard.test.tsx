import React from 'react';
import renderer from 'react-test-renderer';

import { CONSIDERATION_ISSUE_LEVEL } from '~/enums/ConsiderationIssueLevel';

import {
    CandidateCard, ICandidateCardProps,
} from '.';

const baseCard = {
    id: 1,
    extended_status: 'hr_screening_finished',
    extended_status_changed_at: '2018-09-13T10:26:24.534475Z',
    resolution: '',
    state: 'in_progress',
};

const baseCandidate = {
    first_name: 'Ольга',
    last_name: 'Козлова',
    id: 1,
    is_current_employee: false,
};

describe('Jest snapshot тесты на компонент CandidateCard', () => {
    it('Кандидат без логина', () => {
        const card: ICandidateCardProps = {
            ...baseCard,
            // @ts-ignore
            candidate: baseCandidate,
        };
        const CandidateCardComponent = renderer.create(
            <CandidateCard
                {...card}
            />).toJSON();
        expect(CandidateCardComponent).toMatchSnapshot();
    });

    it('Кандидат действующий сотрудник', () => {
        const card: ICandidateCardProps = {
            ...baseCard,
            // @ts-ignore
            candidate: {
                ...baseCandidate,
                login: 'olgakozlova',
                is_current_employee: true,
            },
        };
        const CandidateCardComponent = renderer.create(
            <CandidateCard
                {...card}
            />).toJSON();
        expect(CandidateCardComponent).toMatchSnapshot();
    });

    it('Кандидат бывший сотрудник', () => {
        const card: ICandidateCardProps = {
            ...baseCard,
            // @ts-ignore
            candidate: {
                login: 'olgakozlova',
                ...baseCandidate,
            },
        };
        const CandidateCardComponent = renderer.create(
            <CandidateCard
                {...card}
            />).toJSON();
        expect(CandidateCardComponent).toMatchSnapshot();
    });

    it('Кандидат с профессиями < 2', () => {
        const card: ICandidateCardProps = {
            ...baseCard,
            // @ts-ignore
            candidate: {
                ...baseCandidate,
                professions: [
                    {
                        id: 1,
                        name: 'Разработчик бекэнда',
                    },
                ],
            },
        };
        const CandidateCardComponent = renderer.create(
            <CandidateCard
                {...card}
            />).toJSON();
        expect(CandidateCardComponent).toMatchSnapshot();
    });
    it('Кандидат с профессиями >= 2', () => {
        const card: ICandidateCardProps = {
            ...baseCard,
            // @ts-ignore
            candidate: {
                ...baseCandidate,
                professions: [
                    {
                        id: 1,
                        name: 'Разработчик бекэнда',
                    },
                    {
                        id: 2,
                        name: 'Разработчик фронтенда',
                    },
                ],
            },
        };
        const CandidateCardComponent = renderer.create(
            <CandidateCard
                {...card}
            />).toJSON();
        expect(CandidateCardComponent).toMatchSnapshot();
    });
    it('Кандидат с городами < 2', () => {
        const card: ICandidateCardProps = {
            ...baseCard,
            // @ts-ignore
            candidate: {
                ...baseCandidate,
                target_cities: [
                    {
                        id: 1,
                        name: 'Минск',
                    },
                ],
            },
        };
        const CandidateCardComponent = renderer.create(
            <CandidateCard
                {...card}
            />).toJSON();
        expect(CandidateCardComponent).toMatchSnapshot();
    });
    it('Кандидат с городами >= 2', () => {
        const card: ICandidateCardProps = {
            // @ts-ignore
            candidate: {
                ...baseCandidate,
                target_cities: [
                    {
                        id: 1,
                        name: 'Минск',
                    },
                    {
                        id: 2,
                        name: 'Брест',
                    },
                ],
            },
            extended_status_changed_at: '2021-04-23T17:42:34.360023Z',
        };
        const CandidateCardComponent = renderer.create(
            <CandidateCard
                {...card}
            />).toJSON();
        expect(CandidateCardComponent).toMatchSnapshot();
    });
    it('Кандидат только со скринингами', () => {
        const card: ICandidateCardProps = {
            // @ts-ignore
            candidate: { ...baseCandidate },
            interviews: [
                {
                    grade: null,
                    id: 31632,
                    resolution: 'nohire',
                    state: 'finished',
                    type: 'hr_screening',
                    yandex_grade: null,
                },
                {
                    grade: null,
                    id: 31634,
                    resolution: 'hire',
                    state: 'finished',
                    type: 'hr_screening',
                    yandex_grade: null,
                },
            ],
            extended_status_changed_at: '2021-04-23T17:42:34.360023Z',
        };
        const CandidateCardComponent = renderer.create(
            <CandidateCard
                {...card}
            />).toJSON();
        expect(CandidateCardComponent).toMatchSnapshot();
    });
    it('Кандидат с прочими видами интервью', () => {
        const card: ICandidateCardProps = {
            // @ts-ignore
            candidate: { ...baseCandidate },
            interviews: [
                {
                    grade: null,
                    id: 31632,
                    resolution: 'nohire',
                    state: 'finished',
                    type: 'hr_screening',
                    yandex_grade: null,
                },
                {
                    id: 33840,
                    type: 'screening',
                    grade: null,
                    state: 'assigned',
                    resolution: '',
                    yandex_grade: null,
                },
                {
                    grade: 0,
                    id: 31036,
                    resolution: '',
                    state: 'estimated',
                    type: 'screening',
                    yandex_grade: null,
                },
                {
                    id: 32751,
                    type: 'screening',
                    grade: 3,
                    state: 'finished',
                    resolution: '',
                    yandex_grade: null,
                },
                {
                    id: 31907,
                    type: 'aa',
                    grade: 4,
                    state: 'finished',
                    resolution: '',
                    yandex_grade: null,
                },
                {
                    grade: 0,
                    id: 31038,
                    resolution: '',
                    state: 'finished',
                    type: 'aa',
                    yandex_grade: null,
                },
                {
                    id: 319407,
                    type: 'aa',
                    grade: null,
                    state: 'assigned',
                    resolution: '',
                    yandex_grade: null,
                },
                {
                    id: 31210,
                    type: 'regular',
                    grade: 2,
                    state: 'finished',
                    resolution: '',
                    yandex_grade: null,
                },
                {
                    id: 321210,
                    type: 'regular',
                    grade: 0,
                    state: 'finished',
                    resolution: '',
                    yandex_grade: null,
                },
                {
                    id: 326210,
                    type: 'regular',
                    grade: null,
                    state: 'assigned',
                    resolution: '',
                    yandex_grade: null,
                },
                {
                    id: 31944,
                    type: 'final',
                    grade: null,
                    state: 'finished',
                    resolution: 'hire',
                    yandex_grade: null,
                },
                {
                    id: 319444,
                    type: 'final',
                    grade: null,
                    state: 'finished',
                    resolution: 'nohire',
                    yandex_grade: null,
                },
                {
                    id: 319444,
                    type: 'final',
                    grade: null,
                    state: 'assigned',
                    resolution: '',
                    yandex_grade: null,
                },
            ],
            extended_status_changed_at: '2021-04-23T17:42:34.360023Z',
        };
        const CandidateCardComponent = renderer.create(
            <CandidateCard
                {...card}
            />).toJSON();
        expect(CandidateCardComponent).toMatchSnapshot();
    });
    it('Кандидат c issue на дату изменения статуса', () => {
        const card: ICandidateCardProps = {
            // @ts-ignore
            candidate: {
                ...baseCandidate,
                issues: [
                    {
                        id: 1,
                        type: 'extended_status_changed_at',
                        level: CONSIDERATION_ISSUE_LEVEL.DANGER,
                        params: {},
                    },
                ],
            },
            extended_status_changed_at: '2021-04-23T17:42:34.360023Z',
        };
        const CandidateCardComponent = renderer.create(
            <CandidateCard
                {...card}
            />).toJSON();
        expect(CandidateCardComponent).toMatchSnapshot();
    });
});
