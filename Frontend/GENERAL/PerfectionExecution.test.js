import React from 'react';
import { render } from 'enzyme';

import { PerfectionExecution } from './PerfectionExecution';
import { getExecution } from './PerfectionExecution.stories';

Date.now = jest.fn(() => new Date('2019-03-10T02:00:00Z'));

describe('PerfectionExecution', () => {
    it('with today expire', () => {
        const wrapper = render(
            <PerfectionExecution
                execution={getExecution(0)}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with tomorrow expire', () => {
        const wrapper = render(
            <PerfectionExecution
                execution={getExecution(1)}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with 2 days expire', () => {
        const wrapper = render(
            <PerfectionExecution
                execution={getExecution(2)}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with 5 days expire', () => {
        const wrapper = render(
            <PerfectionExecution
                execution={getExecution(5)}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with 21 days expire', () => {
        const wrapper = render(
            <PerfectionExecution
                execution={getExecution(21)}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('expired 2 days ago', () => {
        const wrapper = render(
            <PerfectionExecution
                execution={getExecution(0, -2)}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('expired 1 days ago', () => {
        const wrapper = render(
            <PerfectionExecution
                execution={getExecution(0, -1)}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('expired today', () => {
        const wrapper = render(
            <PerfectionExecution
                execution={getExecution(0, 0)}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('expired 2 days ago, but not applied yet', () => {
        const wrapper = render(
            <PerfectionExecution
                execution={getExecution(-2)}
            />
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with threat', () => {
        const wrapper = render(
            <PerfectionExecution
                execution={getExecution(-2)}
                threat
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
