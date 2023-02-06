import React from 'react';
import { mount } from 'enzyme';
import { addTranslation, setTankerProjectId } from 'react-tanker';

import { withRegistry } from '@bem-react/di';

import registry from '../../../../registry';

import ListBase from '../../../../../../components/notification-list';
import ListExp from '../../../../components/notification-list';
import NotificationCenterContext from '../../../../../../components/notification-center/context';

const List = withRegistry(registry)(ListExp(ListBase));

setTankerProjectId('global_notifications');
addTranslation(LANG, require('../../../../../../i18n/' + LANG + '.js'));

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

describe('Notification list', () => {
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
                .children()
                .children().length,
        ).toBe(notificationWithToday.length);
        expect(component.find('.gnc-notification-list__date-label').length).toBe(0);
    });
});
