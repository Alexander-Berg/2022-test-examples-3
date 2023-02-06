import React from 'react';
import { shallow } from 'enzyme';
import inherit from 'inherit';

import TaSuggest from 'b:ta-suggest';
import { SchedulesSettings } from './SchedulesSettings';

describe('Should render', () => {
    it('schedules form with default schedules', () => {
        inherit.self(TaSuggest, {}, {
            _fetch: () => Promise.resolve(),
        });

        const wrapper = shallow(
            <SchedulesSettings
                service={{ id: 42 }}
                onChange={() => {
                }}
                onAdd={() => {
                }}
                isSlugEditable={() => true}
                getPersonsError={() => {
                }}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('schedules form', () => {
        inherit.self(TaSuggest, {}, {
            _fetch: () => Promise.resolve(),
        });

        const wrapper = shallow(
            <SchedulesSettings
                service={{ id: 42 }}
                schedules={[
                    {
                        id: 1,
                        name: 'release',
                        slug: 'release',
                        role: null,
                        personsCount: 1,
                        considerOtherSchedules: true,
                        duration: 5,
                        dutyOnHolidays: true,
                        dutyOnWeekends: true,
                        startDate: new Date('2019-01-01'),
                        startTime: '12:34',
                        needOrder: true,
                        autoapproveTimedelta: 14,
                    }]}
                onChange={() => {
                }}
                onAdd={() => {
                }}
                isSlugEditable={() => false}
                getPersonsError={() => {
                }}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('schedules form with multiple schedules', () => {
        inherit.self(TaSuggest, {}, {
            _fetch: () => Promise.resolve(),
        });

        const schedules = [
            {
                id: 1,
                name: 'release',
                slug: 'release',
                role: {
                    id: 42,
                    name: {
                        ru: 'Разаботчик интерфейсов',
                        en: 'Frontend developer',
                    },
                },
                personsCount: 2,
                considerOtherSchedules: false,
                duration: 2,
                dutyOnHolidays: true,
                dutyOnWeekends: true,
                startDate: new Date('2019-04-01'),
                startTime: '20:20',
                needOrder: false,
            },
            {
                id: 2,
                name: 'dev',
                slug: 'dev',
                role: {
                    id: 43,
                    name: {
                        ru: 'Разаботчик интерфейсов',
                        en: 'Frontend developer',
                    },
                },
                personsCount: 42,
                considerOtherSchedules: false,
                duration: 5,
                dutyOnHolidays: true,
                dutyOnWeekends: true,
                startDate: new Date('2019-01-01'),
                startTime: '00:01',
                needOrder: true,
                autoapproveTimedelta: 12,
            },
        ];

        const wrapper = shallow(
            <SchedulesSettings
                service={{ id: 42 }}
                schedules={schedules}
                propsSchedule={schedules}
                onChange={() => {
                }}
                onAdd={() => {
                }}
                isSlugEditable={() => false}
                getPersonsError={() => {
                }}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });

    it('schedules form with error', () => {
        inherit.self(TaSuggest, {}, {
            _fetch: () => Promise.resolve(),
        });

        const wrapper = shallow(
            <SchedulesSettings
                service={{ id: 42 }}
                schedules={[
                    {
                        id: 1,
                        name: 'release',
                        slug: 'release',
                        role: {
                            id: 42,
                            name: {
                                ru: 'Разаботчик интерфейсов',
                                en: 'Frontend developer',
                            },
                        },
                        personsCount: 2,
                        considerOtherSchedules: false,
                        duration: 2,
                        dutyOnHolidays: true,
                        dutyOnWeekends: true,
                        startDate: new Date('2019-04-01'),
                        startTime: '12:34',
                        needOrder: false,
                        autoapproveTimedelta: 14,
                    },
                    {
                        id: 2,
                        name: 'dev',
                        slug: 'dev',
                        role: {
                            id: 43,
                            name: {
                                ru: 'Разаботчик интерфейсов',
                                en: 'Frontend developer',
                            },
                        },
                        personsCount: 4,
                        considerOtherSchedules: false,
                        duration: 5,
                        dutyOnHolidays: true,
                        dutyOnWeekends: true,
                        startDate: new Date('2019-01-01'),
                        startTime: '12:34',
                        needOrder: true,
                        autoapproveTimedelta: 12,
                    }]}
                onChange={() => {
                }}
                onAdd={() => {
                }}
                isSlugEditable={() => false}
                errors={[null, new Error('Error')]}
                getPersonsError={() => {
                }}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
