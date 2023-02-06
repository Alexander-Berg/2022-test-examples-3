import React from 'react';
import { mount } from 'enzyme';
import { addTranslation, setTankerProjectId } from 'react-tanker';

import Settings from '../../../../src/components/settings';
import NotificationCenterContext from '../../../../src/components/notification-center/context';

const services = [
    {
        recordId: '1',
        name: 'serviceName',
        settings: {
            s1: {
                enabled: true,
                text: 'all',
            },
        },
    },
];

setTankerProjectId('global_notifications');
addTranslation(LANG, require('../../../../src/i18n/' + LANG + '.js'));

describe('Settings', () => {
    describe('Render with `gnc-settings_closed` class name', () => {
        test('should render empty settings ', () => {
            const component = mount(
                <Settings />
            );

            expect(component.html()).toMatchSnapshot();
        });

        test('should render not empty settings', () => {
            const component = mount(
                <Settings services={services} countOfNotificationsByService={{ '1': true }} />
            );

            expect(component.html()).toMatchSnapshot();
        });

        test('should not render settings from fake services', () => {
            const component = mount(
                <Settings
                    services={services}
                    countOfNotificationsByService={{ '1': true }}
                    countOfRealNotificationsByService={{ '1': 0 }} />
            );

            expect(component.html()).toMatchSnapshot();
        });
    });

    describe('Render with `gnc-settings_open` class name', () => {
        test('should render empty settings list', () => {
            const component = mount(
                <Settings settingsAreOpen />
            );

            expect(component.html()).toMatchSnapshot();
        });

        test('should render not empty settings list', () => {
            const component = mount(
                <Settings services={services} settingsAreOpen countOfNotificationsByService={{ '1': true }} />
            );

            expect(component.html()).toMatchSnapshot();
        });

        test('should render settings list without `gnc-settings__header_open` class name', () => {
            const component = mount(
                <Settings services={services} settingsAreOpen countOfNotificationsByService={{ '1': true }} />
            );

            const headerSetting1 = component.find('.gnc-settings__header-1');
            headerSetting1.simulate('click');

            expect(component.html()).toMatchSnapshot();
        });

        test('should render settings list without `gnc-settings__checkbox-box_checked` class name on checkbox', () => {
            const dispatch = () => {};
            const getState = () => ({ config: {} });
            const countSettings = () => {};
            const clckCounter = () => {};
            const context = { store: { dispatch, getState }, metrika: { countSettings }, clckCounter };
            const component = mount(
                <NotificationCenterContext.Provider value={context}>
                    <Settings services={services} settingsAreOpen countOfNotificationsByService={{ '1': true }} />
                </NotificationCenterContext.Provider>
            );

            const headerSetting1 = component.find('.gnc-settings__checkbox');
            headerSetting1.instance().checked = false;
            headerSetting1.simulate('change');

            expect(component.html()).toMatchSnapshot();
        });
    });
});
