import React from 'react';
import { mount } from 'enzyme';
import { addTranslation, setTankerProjectId } from 'react-tanker';

import { withRegistry } from '@bem-react/di';

import registry from '../../../../src/components/registry';

import ListBase from '../../../../src/components/notification-list/index';
import NotificationCenterContext from '../../../../src/components/notification-center/context';
import NotificationGroup from '../../../../src/components/notification-group';
import Notification from '../../../../src/components/notification';
import ExpandButton from '../../../../src/components/expand-button';

const List = withRegistry(registry)(ListBase);

setTankerProjectId('global_notifications');
addTranslation(LANG, require('../../../../src/i18n/' + LANG + '.js'));

const services = [
    {
        recordId: 'znatoki',
        block_collection: 'znatoki',
        icon_src: 'https://avatars.mds.yandex.net/get-ya_notification_center/872891/znatoki-icon/svg',
    },
    {
        recordId: 'video',
        block_collection: 'video',
        icon_src: 'https://avatars.mds.yandex.net/get-ya_notification_center/397487/video-icon/svg',
    },
];

const getState = () => ({ services, config: { logging: { exp_boxes: '123' } } });
const dispatch = jest.fn(() => Promise.resolve());

const context = {
    store: { getState, dispatch },
    metrika: { countClick: jest.fn(), countActions: jest.fn() },
    clckCounter: jest.fn(),
};

const serviceNames = services.map(s => s.recordId);

const recordId = 123456789;
const notification1 = {
    service: serviceNames[0],
    collectionId: serviceNames[0],
    recordId,
    is_read: false,
    action_link: '//yandex.ru/action',
    mtime: 1565973051997 - 86400000 * 10,
    message: [{ type: 'user', text: 'Harold' }, ' says he is tired…'],
    meta: { actor: { link: '//yandex.ru' }, counterParams: { custom: 'param' } },
};
const notification2 = Object.assign({}, notification1, { is_read: true, recordId: recordId + 1 });
const notification3 = Object.assign({}, notification1, { recordId: recordId + 2, service: serviceNames[1] });

const notifications = [notification1, notification2, notification3];

function generateServices(count) {
    const generatedServices = [];
    for (let i = 0; i < count; ++i) {
        generatedServices.push({
            icon_src: '//yandex.ru/icon',
            recordId: 'service' + i,
        });
    }
    return generatedServices;
}

function generateNotifications(count, getServiceName = i => `service${i}`, getRecordId = i => `${i}`) {
    const generatedNotifications = [];
    for (let i = 0; i < count; ++i) {
        generatedNotifications.push(
            Object.assign({}, notification1, {
                recordId: getRecordId(i),
                service: getServiceName(i),
            }),
        );
    }
    return generatedNotifications;
}

function generateViewedNotifications(count, getServiceName = i => `service${i}`, getRecordId = i => `${i}`) {
    const generatedNotifications = [];
    for (let i = 0; i < count; ++i) {
        generatedNotifications.push(
            Object.assign({}, notification2, {
                recordId: getRecordId(i),
                service: getServiceName(i),
            }),
        );
    }
    return generatedNotifications;
}

describe('Notification list', () => {
    test('empty', () => {
        const component = mount(
            <NotificationCenterContext.Provider value={context}>
                <List hasNotifications={false} />
            </NotificationCenterContext.Provider>
        );

        expect(component.html()).toMatchSnapshot();
    });

    test('filter = all', () => {
        const component = mount(
            <NotificationCenterContext.Provider value={context}>
                <List services={services} notifications={notifications} />
            </NotificationCenterContext.Provider>,
        );

        expect(component.find(Notification).length).toBe(notifications.length);
        expect(component.find(NotificationGroup).length).toBe(0);
    });

    test(`filter = ${serviceNames[0]}`, () => {
        const component = mount(
            <NotificationCenterContext.Provider value={context}>
                <List services={services} notifications={notifications} filter={serviceNames[0]} />
            </NotificationCenterContext.Provider>,
        );

        const filteredNotifications = notifications.filter(n => n.service === serviceNames[0]);

        expect(component.find(Notification).length).toBe(filteredNotifications.length);
        expect(component.find('.gnc-notification-list__date-label').length).toBe(0);
        expect(component.find(NotificationGroup).length).toBe(0);
    });

    test('date label for today and later', () => {
        const notificationWithToday = [
            Object.assign({}, notification1, { mtime: Date.now() }),
            ...notifications.slice(1),
        ];

        const component = mount(
            <NotificationCenterContext.Provider value={context}>
                <List services={services} notifications={notificationWithToday} />
            </NotificationCenterContext.Provider>,
        );

        expect(
            component
                .children()
                .children()
                .children().length,
        ).toBe(notificationWithToday.length + 2);
        expect(component.find('.gnc-notification-list__date-label').length).toBe(2);
        expect(component.find('.gnc-notification-list__later').length).toBe(1);
        expect(component.find(NotificationGroup).length).toBe(0);
    });

    test('only date label for today', () => {
        const notificationWithToday = notifications.map(n => Object.assign({}, n, { mtime: Date.now() }));

        const component = mount(
            <NotificationCenterContext.Provider value={context}>
                <List services={services} notifications={notificationWithToday} />
            </NotificationCenterContext.Provider>,
        );

        expect(
            component
                .children()
                .children()
                .children().length,
        ).toBe(notificationWithToday.length + 1);
        expect(component.find('.gnc-notification-list__date-label').length).toBe(1);
        expect(component.find('.gnc-notification-list__later').length).toBe(0);
        expect(component.find(NotificationGroup).length).toBe(0);
    });

    test('render 14 notifications using NotificationGroup', () => {
        const COUNT_NOTIFICATIONS = 14;
        const countOfNotificationsByService = services.reduce((acc, item) => {
            acc[item.recordId] = COUNT_NOTIFICATIONS;
            return acc;
        }, {});

        const part = parseInt(COUNT_NOTIFICATIONS / 2);
        const generatedNotifications = [
            ...generateNotifications(
                part,
                () => serviceNames[0],
                i => `1-${i}`,
            ),
            ...generateNotifications(
                COUNT_NOTIFICATIONS - part,
                () => serviceNames[1],
                i => `3-${i}`,
            ),
        ];

        const component = mount(
            <NotificationCenterContext.Provider value={context}>
                <List
                    notifications={generatedNotifications}
                    countOfNotificationsByService={countOfNotificationsByService}
                    services={services}
                />
            </NotificationCenterContext.Provider>,
        );

        expect(component.find(NotificationGroup).length).toBe(2);
        expect(
            component
                .children()
                .children()
                .children().length,
        ).toBe(6);
    });

    test('log feed layout after ExpandButton click', () => {
        const COUNT_NOTIFICATIONS = 14;
        const countOfNotificationsByService = services.reduce((acc, item) => {
            acc[item.recordId] = COUNT_NOTIFICATIONS;
            return acc;
        }, {});

        const part = parseInt(COUNT_NOTIFICATIONS / 2);
        const generatedNotifications = [
            ...generateNotifications(
                part,
                () => serviceNames[0],
                i => `1-${i}`,
            ),
            ...generateNotifications(
                COUNT_NOTIFICATIONS - part,
                () => serviceNames[1],
                i => `3-${i}`,
            ),
        ];

        const clck = jest.fn();
        const clckContext = Object.assign({}, context, { clckCounter: clck });

        const component = mount(
            <NotificationCenterContext.Provider value={clckContext}>
                <List
                    notifications={generatedNotifications}
                    countOfNotificationsByService={countOfNotificationsByService}
                    services={services}
                />
            </NotificationCenterContext.Provider>,
        );

        component
            .find(ExpandButton)
            .at(0)
            .find('.gnc-expand-button__content')
            .simulate('click');

        expect(clck).nthCalledWith(1, {
            cid: 73275,
            path: '3027.241.3626',
            vars: {
                exp_boxes: '123',
                filter: '',
                windowHeight: 768,
                blocks: JSON.stringify([
                    { service: 'znatoki', block_id: '1-0', is_new: 0, is_read: 0, position: 1, height: 0, isVisible: true },
                    { service: 'znatoki', block_id: '1-1', is_new: 0, is_read: 0, position: 2, height: 0, isVisible: true  },
                    { service: 'znatoki', type: 'group_5', block_id: '0later7', position: 3, height: 0, isVisible: true  },
                    { service: 'video', block_id: '3-0', is_new: 0, is_read: 0, position: 4, height: 0, isVisible: true  },
                    { service: 'video', block_id: '3-1', is_new: 0, is_read: 0, position: 5, height: 0, isVisible: true  },
                    { service: 'video', type: 'group_5', block_id: '1later7', position: 6, height: 0, isVisible: true  },
                ]),
                count: 6,
            },
        });
    });

    test('render 60 notifications from 21 services, ExpandButton is visible', () => {
        jest.useFakeTimers();
        const COUNT_SERVICES = 20;
        const COUNT_NOTIFICATIONS = 3;
        const generatedServices = generateServices(COUNT_SERVICES);
        const countOfNotificationsByService = generatedServices.reduce((acc, item) => {
            acc[item.recordId] = COUNT_NOTIFICATIONS;
            return acc;
        }, {});

        const generatedNotifications = generatedServices.reduce(
            (acc, item, i) =>
                acc.concat(
                    generateNotifications(
                        COUNT_NOTIFICATIONS,
                        () => item.recordId,
                        j => `${i}-${j}`,
                    ),
                ),
            [],
        );

        const component = mount(
            <NotificationCenterContext.Provider value={context}>
                <List
                    notifications={generatedNotifications}
                    countOfNotificationsByService={countOfNotificationsByService}
                    services={generatedServices}
                />
            </NotificationCenterContext.Provider>,
        );

        jest.runAllTimers();

        expect(component.find(Notification).length).toBe(21);
        expect(
            component
                .children()
                .children()
                .children().length,
        ).toBe(22);
        expect(
            component
                .children()
                .children()
                .childAt(21)
                .html(),
        ).toMatchSnapshot();

        jest.clearAllTimers();
    });

    test('render viewed 60 notifications from 21 services, ExpandButton is visible, no unreaded notifications', () => {
        jest.useFakeTimers();
        const COUNT_SERVICES = 20;
        const COUNT_NOTIFICATIONS = 3;
        const generatedServices = generateServices(COUNT_SERVICES);
        const countOfNotificationsByService = generatedServices.reduce((acc, item) => {
            acc[item.recordId] = COUNT_NOTIFICATIONS;
            return acc;
        }, {});

        const generatedNotifications = generatedServices.reduce(
            (acc, item, i) =>
                acc.concat(
                    generateViewedNotifications(
                        COUNT_NOTIFICATIONS,
                        () => item.recordId,
                        j => `${i}-${j}`,
                    ),
                ),
            [],
        );

        const component = mount(
            <NotificationCenterContext.Provider value={context}>
                <List
                    notifications={generatedNotifications}
                    countOfNotificationsByService={countOfNotificationsByService}
                    services={generatedServices}
                />
            </NotificationCenterContext.Provider>,
        );

        jest.runAllTimers();

        expect(component.hasClass('gnc-notifications-item-wrapper_not-viewed')).toBeFalsy();

        expect(
            component
                .children()
                .children()
                .childAt(21)
                .html(),
        ).toMatchSnapshot();

        jest.clearAllTimers();
    });

    test(`render 14 notifications with filter = ${serviceNames[0]}`, () => {
        const COUNT_NOTIFICATIONS = 14;
        const countOfNotificationsByService = services.reduce((acc, item) => {
            acc[item.recordId] = COUNT_NOTIFICATIONS;
            return acc;
        }, {});

        const part = parseInt(COUNT_NOTIFICATIONS / 2);
        const generatedNotifications = [
            ...generateNotifications(
                part,
                () => serviceNames[0],
                i => `1-${i}`,
            ),
            ...generateNotifications(
                COUNT_NOTIFICATIONS - part,
                () => serviceNames[1],
                i => `3-${i}`,
            ),
        ];

        const component = mount(
            <NotificationCenterContext.Provider value={context}>
                <List
                    notifications={generatedNotifications}
                    countOfNotificationsByService={countOfNotificationsByService}
                    services={services}
                    filter={serviceNames[0]}
                />
            </NotificationCenterContext.Provider>,
        );

        const filteredNotifications = generatedNotifications.filter(n => n.service === serviceNames[0]);

        expect(component.find(Notification).length).toBe(filteredNotifications.length);
        expect(component.find('.gnc-notification-list__date-label').length).toBe(0);
    });
});
