import React from 'react';
import { render } from 'enzyme';

import PerfectionDetails from './PerfectionDetails';

jest.mock('./Content/Content');

const issueGroups = [
    {
        name: { ru: 'Понятность', en: 'Understandability' },
        issuesCount: 1,
        code: 'understandability',
        issues: [{
            canBeAppealed: false,
            description: { ru: 'Руководитель текущего сервиса находится в разных службах с руководителем вышестоящего сервис.' },
            execution: {
                name: { ru: 'Переведение сервиса в статус Требуется информация' },
                applyDate: new Date('2019-09-17T00:15:16Z'),
            }, id: 1661,
            onReview: false,
            name: { ru: 'Руководитель вне кадровой иерархии' },
            recommendation: { ru: 'Назначьте другого руководителя или переместите сервис в нужное место по иерархии.' },
            weight: 10,
        }],
        description: { ru: 'Сервис находится в желтой зоне по количеству проблем в группе Команда' },
        recommendation: { ru: 'Исправьте проблемы в сервисе и не допускайте новыx' },
        summary: {
            currentLevel: 'warning',
            currentWeight: 55,
            criticalWeight: 33,
            warningWeight: 66,
            currentExecution: {
                name: { ru: 'Переведение сервиса в статус Требуется информация' },
                applyDate: new Date('2019-10-17T00:15:16Z'),
                isCritical: true,
            },
            nextExecution: {
                name: { ru: 'Сервис перейдет в красную зону при достижении порога 33% и будет Закрыт' },
                applyDate: new Date('2019-09-17T00:15:16Z'),
                isCritical: true,
            },
        },
    },
    {
        name: { ru: 'Вложенные сервисы', en: 'Children' },
        issuesCount: 0,
        code: 'children',
    },
];

const complaints = [
    {
        createdAt: new Date('2019-09-11T14:11:25.055800Z'),
        message: 'Problem1',
    },
    {
        createdAt: new Date('2019-09-11T14:30:47.575142Z'),
        message: 'Problem2',
    },
];

describe('Should render PerfectionDetails', () => {
    it('default', () => {
        const wrapper = render(
            <PerfectionDetails
                issueGroupsLoading={false}
                issueGroups={issueGroups}
                complaints={complaints}
                issueGroupsError={null}
                complaintsError={null}
                complaintsLoading={false}
                queryObj={{}}
                updateServiceOptions={jest.fn()}
                updateQueryStr={jest.fn()}
                issuesGroup="understandability"
                issuesPage={1}
                issuesPageSize={10}
                openGroup={jest.fn()}
                canFixIssues={false}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('empty', () => {
        const wrapper = render(
            <PerfectionDetails
                issueGroupsLoading={false}
                issueGroups={[]}
                complaints={[]}
                issueGroupsError={null}
                complaintsError={null}
                complaintsLoading={false}
                queryObj={{}}
                updateServiceOptions={jest.fn()}
                updateQueryStr={jest.fn()}
                issuesGroup="understandability"
                issuesPage={1}
                issuesPageSize={10}
                openGroup={jest.fn()}
                canFixIssues={false}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with issueGroupError', () => {
        const wrapper = render(
            <PerfectionDetails
                issueGroupsLoading={false}
                issueGroups={[]}
                complaints={[]}
                issueGroupsError={{ data: { extra: new Error('Error') } }}
                complaintsError={null}
                complaintsLoading={false}
                queryObj={{}}
                updateServiceOptions={jest.fn()}
                updateQueryStr={jest.fn()}
                issuesGroup="understandability"
                issuesPage={1}
                issuesPageSize={10}
                openGroup={jest.fn()}
                canFixIssues={false}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with loading', () => {
        const wrapper = render(
            <PerfectionDetails
                issueGroupsLoading
                issueGroups={[]}
                complaints={[]}
                issueGroupsError={null}
                complaintsError={null}
                complaintsLoading={false}
                queryObj={{}}
                updateServiceOptions={jest.fn()}
                updateQueryStr={jest.fn()}
                issuesGroup="understandability"
                issuesPage={1}
                issuesPageSize={10}
                openGroup={jest.fn()}
                canFixIssues={false}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with complaintsError', () => {
        const wrapper = render(
            <PerfectionDetails
                issueGroupsLoading={false}
                issueGroups={issueGroups}
                complaints={[]}
                issueGroupsError={null}
                complaintsError={{ data: { extra: new Error('Error') } }}
                complaintsLoading={false}
                queryObj={{}}
                updateServiceOptions={jest.fn()}
                updateQueryStr={jest.fn()}
                issuesGroup="understandability"
                issuesPage={1}
                issuesPageSize={10}
                openGroup={jest.fn()}
                canFixIssues={false}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
