import React from 'react';
import { render } from 'enzyme';

import { SCOPE_PREFIX } from 'tools-access-react-redux-router/src/configs';

import { withRedux } from '~/src/common/hoc';
import { configureStore } from '~/src/abc/react/redux/store';

import Content from './Content';

jest.mock('../../PerfectionIssueGroup/PerfectionIssueGroup');
jest.mock('../../PerfectionExecution/PerfectionExecution');

const issues = [{
    canBeAppealed: false,
    description: { ru: 'Руководитель текущего сервиса находится в разных службах с руководителем вышестоящего сервис.' },
    execution: {
        name: { ru: 'Сервис перейдет в статус Требуется информация' },
        applyDate: new Date('2019-09-17T00:15:16Z'),
    },
    id: 1661,
    onReview: false,
    name: { ru: 'Руководитель вне кадровой иерархии' },
    recommendation: { ru: 'Назначьте другого руководителя или переместите сервис в нужное место по иерархии.' },
    weight: 10,
}];

const complaints = {
    count: 2,
    totalPages: 1,
    results: [
        {
            createdAt: new Date('2019-09-11T14:11:25.055800Z'),
            message: 'Problem1',
        },
        {
            createdAt: new Date('2019-09-11T14:30:47.575142Z'),
            message: 'Problem2',
        },
    ],
};

const paginatedComplaints = {
    count: 40,
    totalPages: 20,
    results: [
        {
            createdAt: new Date('2019-09-11T14:11:25.055800Z'),
            message: 'Problem1',
        },
        {
            createdAt: new Date('2019-09-11T14:30:47.575142Z'),
            message: 'Problem2',
        },
    ],
};

const issueGroup = {
    name: { ru: 'Понятность' },
    description: { ru: 'Сервис находится в желтой зоне по количеству проблем в группе Команда' },
    recommendation: { ru: 'Исправьте проблемы в сервисе и не допускайте новыx' },
    summary: {
        currentLevel: 'warning',
        currentWeight: 55,
        criticalWeight: 33,
        warningWeight: 66,
        currentExecution: {
            name: { ru: 'Сервис перейдёт в статус Требуется информация' },
            applyDate: new Date('2019-10-17T00:15:16Z'),
            isCritical: true,
        },
        nextExecution: {
            name: { ru: 'Сервис перейдет в красную зону при достижении порога 33% и будет Закрыт' },
            applyDate: new Date('2019-09-17T00:15:16Z'),
            isCritical: true,
        },
    },
};

describe('PerfectionDetails', () => {
    it('with issues content', () => {
        const wrapper = render(
            <Content
                type="Issues"
                values={issues}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with empty content', () => {
        const wrapper = render(
            <Content
                type="Empty"
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with complaints content', () => {
        const wrapper = render(
            <Content
                type="Complaints"
                values={complaints}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with paginated complaints content', () => {
        const store = configureStore({
            initialState: {
                [SCOPE_PREFIX]: '',
            },
            fetcherOptions: {
                fetch: () => Promise.resolve(),
            },
        });

        const ContentConnected = withRedux(Content, store);

        const wrapper = render(
            <ContentConnected
                type="Complaints"
                values={paginatedComplaints}
                issuesPage={1}
                queryObj={{}}
                updateQueryStr={jest.fn()}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with empty complaints content', () => {
        const wrapper = render(
            <Content
                type="Complaints"
                values={{ results: [] }}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with issues content + issue groups', () => {
        const wrapper = render(
            <Content
                type="Issues"
                values={issues}
                issueGroup={issueGroup}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
