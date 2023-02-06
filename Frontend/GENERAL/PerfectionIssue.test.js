import React from 'react';
import { render } from 'enzyme';

import PerfectionIssue from './PerfectionIssue';

jest.mock('../PerfectionExecution/PerfectionExecution');

const execution = {
    name: { ru: 'Сервис перейдет в статус Требуется информация' },
    applyDate: new Date('2019-09-17T00:15:16Z'),
};

const issue = {
    canBeAppealed: false,
    description: { ru: 'Руководитель текущего сервиса находится в разных службах с руководителем вышестоящего сервис.' },
    execution,
    id: 1661,
    onReview: false,
    name: { ru: 'Руководитель вне кадровой иерархии' },
    recommendation: { ru: 'Назначьте другого руководителя или переместите сервис в нужное место по иерархии.' },
    weight: 10,
    isAppealed: false,
};

describe('PerfectionIssue', () => {
    it('without appeal, with execution', () => {
        const wrapper = render(
            <PerfectionIssue
                issue={issue}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with appeal', () => {
        const wrapper = render(
            <PerfectionIssue
                issue={{
                    ...issue,
                    onReview: true,
                    execution: null,
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('without appeal, without execution', () => {
        const wrapper = render(
            <PerfectionIssue
                issue={{
                    ...issue,
                    onReview: false,
                    execution: null,
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('without id and appealed', () => {
        const wrapper = render(
            <PerfectionIssue
                issue={{
                    ...issue,
                    isAppealed: true,
                    id: null,
                }}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
