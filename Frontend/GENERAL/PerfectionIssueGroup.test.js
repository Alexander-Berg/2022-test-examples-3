import React from 'react';
import { render } from 'enzyme';

import { PerfectionIssueGroup } from './PerfectionIssueGroup';
import { LEVEL_CRITICAL, LEVEL_OK, LEVEL_WARNING } from '../../redux/Perfection.actions';

jest.mock('../PerfectionExecution/PerfectionExecution');
jest.mock('../PerfectionProgressBar/PerfectionProgressBar');

const zones = [
    { level: LEVEL_CRITICAL, width: 33 },
    { level: LEVEL_WARNING, width: 33 },
    { level: LEVEL_OK, width: 34 },
];

const issueGroupOk = {
    name: { ru: 'Понятность' },
    description: null,
    recommendation: null,
    summary: {
        currentLevel: 'ok',
        currentWeight: 75,
        zones,
        currentExecution: null,
        nextExecution: {
            name: { ru: 'Сервис перейдет в желтую зону при достижении порога 66% и будет переведен в статус Требуется информация' },
            isCritical: true,
        },
    },
};

const issueGroupWarning = {
    name: { ru: 'Понятность' },
    description: { ru: 'Сервис находится в желтой зоне по количеству проблем в группе Команда' },
    recommendation: { ru: 'Исправьте проблемы в сервисе и не допускайте новыx' },
    summary: {
        currentLevel: 'warning',
        currentWeight: 55,
        zones,
        currentExecution: {
            name: { ru: 'Переведение сервиса в статус Требуется информация' },
            applyDate: new Date('2019-10-17T00:15:16Z'),
            isCritical: true,
        },
        nextExecution: {
            name: { ru: 'Сервис перейдет в красную зону при достижении порога 33% и будет Закрыт' },
            isCritical: true,
        },
    },
};

const issueGroupCritical = {
    name: { ru: 'Понятность' },
    description: { ru: 'Сервис находится в красной зоне зоне по количеству проблем в группе Команда' },
    recommendation: { ru: 'Исправьте проблемы в сервисе и не допускайте новыx' },
    summary: {
        currentLevel: 'critical',
        currentWeight: 22,
        zones,
        currentExecution: {
            name: { ru: 'Переведение сервиса в статус Требуется информация' },
            applyDate: new Date('2019-10-17T00:15:16Z'),
            isCritical: true,
        },
        nextExecution: null,
    },
};

describe('PerfectionIssueGroup with type', () => {
    it('ok', () => {
        const wrapper = render(
            <PerfectionIssueGroup
                issueGroup={issueGroupOk}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('warning', () => {
        const wrapper = render(
            <PerfectionIssueGroup
                issueGroup={issueGroupWarning}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('critical', () => {
        const wrapper = render(
            <PerfectionIssueGroup
                issueGroup={issueGroupCritical}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
