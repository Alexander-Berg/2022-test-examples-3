import React from 'react';
import { shallow } from 'enzyme';
import moment from 'moment';

import { PerfectionStamp } from './PerfectionStamp';

jest.mock('../PerfectionExecution/PerfectionExecution');

const getExecution = days => ({
    name: { ru: 'Сервис перейдёт в статус Требуется информация' },
    applyDate: moment('2019-09-30').add(days, 'd').toDate(),
});

const getIssue = executionDays => ({
    name: { ru: 'issue name' },
    description: { ru: 'issue description' },
    recommendation: { ru: 'issue recommendation' },
    execution: getExecution(executionDays),
    weight: 10,
});

describe('PerfectionsStamp', () => {
    it('ok level', () => {
        const wrapper = shallow(
            <PerfectionStamp
                issue={null}
                issuesCount={0}
                serviceDataStatus="inited"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('warning level 1 without execution', () => {
        const wrapper = shallow(
            <PerfectionStamp
                issuesCount={1}
                issue={null}
                serviceDataStatus="inited"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('warning level 1', () => {
        const wrapper = shallow(
            <PerfectionStamp
                issuesCount={1}
                issue={getIssue(0)}
                serviceDataStatus="inited"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('warning level 3', () => {
        const wrapper = shallow(
            <PerfectionStamp
                issuesCount={3}
                issue={getIssue(0)}
                serviceDataStatus="inited"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('warning level 5', () => {
        const wrapper = shallow(
            <PerfectionStamp
                issuesCount={3}
                issue={getIssue(0)}
                serviceDataStatus="inited"
            />
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
