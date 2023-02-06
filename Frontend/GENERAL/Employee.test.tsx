import React from 'react';
import { mount } from 'enzyme';
import { User } from '~/src/common/context/types';
import { withContext } from '~/src/common/hoc';
import { AbcContext } from '~/src/abc/react/context/abc-context';
import { Employee as EmployeeBase } from './Employee';

describe('Employee', () => {
    const abcContextMock = {
        configs: {
            hosts: {
                centerClient: { protocol: 'https:', hostname: 'center.y-t.ru' },
                staff: { protocol: 'https:', hostname: 'staff.y-t.ru' },
            },
        },
        user: {} as User,
    };

    const Employee = withContext(EmployeeBase, abcContextMock);

    it('Should render employee with full data', () => {
        const employeeParams = {
            login: 'somelogin',
            name: 'Some Name',
            caption: 'Some secondary text',
            counter: 2,
            appContext: AbcContext,
        };
        const wrapper = mount(
            <Employee {...employeeParams} size="s" />,
        );

        const expectedLinkHref = `https://staff.y-t.ru/${employeeParams.login}`;
        const expectedImgSrc = `https://center.y-t.ru/api/v1/user/${employeeParams.login}/avatar/60.jpg`;

        expect(wrapper.find('.Link.Employee-Name').prop('href')).toBe(expectedLinkHref);
        expect(wrapper.find('.Link.Employee-Name').text()).toBe(employeeParams.name);
        expect(wrapper.find('.Employee-Caption').text()).toBe(employeeParams.caption);
        expect(wrapper.find('.Ticker-Value').text()).toBe(employeeParams.counter.toString());
        expect(wrapper.find('.Employee-Avatar').hostNodes().prop('src')).toBe(expectedImgSrc);
    });

    it('Should render employee with full data and substituted default props', () => {
        const employeeParams = {
            login: 'somelogin',
            name: 'Some Name',
            caption: 'Some secondary text',
            counter: 2,
            appContext: AbcContext,
        };
        const wrapper = mount(
            <Employee {...employeeParams} />,
        );

        const expectedLinkHref = `https://staff.y-t.ru/${employeeParams.login}`;
        const expectedImgSrc = `https://center.y-t.ru/api/v1/user/${employeeParams.login}/avatar/80.jpg`;

        expect(wrapper.find('.Link.Employee-Name').prop('href')).toBe(expectedLinkHref);
        expect(wrapper.find('.Link.Employee-Name').text()).toBe(employeeParams.name);
        expect(wrapper.find('.Employee-Caption').text()).toBe(employeeParams.caption);
        expect(wrapper.find('.Ticker-Value').text()).toBe(employeeParams.counter.toString());
        expect(wrapper.find('.Employee-Avatar').hostNodes().prop('src')).toBe(expectedImgSrc);
    });

    it('Should render minimal employee data with just required props', () => {
        const employeeParams = {
            login: '',
            name: 'Some Name',
            appContext: AbcContext,
        };
        const wrapper = mount(
            <Employee {...employeeParams} />,
        );

        const expectedImgSrc = 'https://center.y-t.ru/api/v1/user/0/avatar/80.jpg';

        expect(wrapper.find('.Link.Employee-Name').length).toBe(0);
        expect(wrapper.find('.Employee-Name').text()).toBe(employeeParams.name);
        expect(wrapper.find('.Employee-Caption').length).toBe(0);
        expect(wrapper.find('.Ticker-Value').length).toBe(0);
        expect(wrapper.find('.Employee-Avatar').hostNodes().prop('src')).toBe(expectedImgSrc);
    });
});
