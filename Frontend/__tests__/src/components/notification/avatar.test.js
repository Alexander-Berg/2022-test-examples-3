import React from 'react';
import { mount } from 'enzyme';

import Avatar from '../../../../src/components/notification/avatar';
import NotificationCenterContext from '../../../../src/components/notification-center/context';

describe('Avatar', () => {
    describe('plain', () => {
        const getState = () => ({ services: [
            {
                block_collection: 'znatoki',
                icon_src: 'https://avatars.mds.yandex.net/get-ya_notification_center/872891/znatoki-icon/svg',
            },
        ] });
        const context = { store: { getState } };

        test('should render user avatar', () => {
            const component = mount(
                <NotificationCenterContext.Provider value={context}>
                    <Avatar
                        notification={{
                            collectionId: 'znatoki',
                            meta: {
                                actor: {
                                    type: 'user',
                                    avatar: 'https://avatars.mds.yandex.net/get-yapic/1/1/islands-retina-middle',
                                },
                            },
                            action_link: '//yandex.ru',
                        }} />
                </NotificationCenterContext.Provider>
            );

            expect(component.html()).toMatchSnapshot();
        });

        test('should render service avatar', () => {
            const component = mount(
                <NotificationCenterContext.Provider value={context}>
                    <Avatar
                        notification={{
                            collectionId: 'znatoki',
                            meta: { actor: { type: 'service' } },
                            action_link: '//yandex.ru',
                        }} />
                </NotificationCenterContext.Provider>
            );

            expect(component.html()).toMatchSnapshot();
        });
    });
});
