/// <reference lib="webworker" />

import { nextTick } from '@yandex-int/messenger.utils/lib/mocks';
import {
    pushText,
    pushTextGroup,
    pushSticker,
    pushFile,
    pushReply,
    pushForward,
    pushMention,
    pushNotApproved,
} from './index.dataset';
const makeServiceWorkerEnv = require('service-worker-mock');

interface ServiceWorkerMock extends ServiceWorkerGlobalScope {
    listeners: Partial<ServiceWorkerGlobalScopeEventMap>;
    cashes: CacheStorage;
    registration: ServiceWorkerRegistration;
    clients: Clients;
    trigger(
        serviceWorkerEvent: keyof ServiceWorkerGlobalScopeEventMap,
        event?: object
    ): void;
    snapshot: void;
}

declare let self: ServiceWorkerMock;
export default null;

const CHAT = '0/0/cbdd62b9-8c01-4f98-b304-3fd34fef87c8';
const FLOYD_CHAT = '0/4/cbdd62b9-8c01-4f98-b304-3fd34fef87c8';
const PRIVATE_CHAT = '28377b9c-af5c-4f5b-8080-801f35d8df93_d40e5dac-43df-4168-a441-0d8aae8d04e6';

describe('Service worker', () => {
    beforeEach(() => {
        Object.assign(global, makeServiceWorkerEnv());
        // @ts-ignore
        const languageGetter = jest.spyOn(global.navigator, 'language', 'get');
        languageGetter.mockReturnValue('ru');
        jest.resetModules();
        require('../../../workers/service-worker.ts');
    });
    it('should add listeners', () => {
        expect(self.listeners.install).toBeDefined();
        expect(self.listeners.activate).toBeDefined();
        expect(self.listeners.push).toBeDefined();
        expect(self.listeners.notificationclick).toBeDefined();
    });
    describe('install', () => {
        it('should install service worker to all clients immediately', async () => {
            self.skipWaiting = jest.fn().mockReturnValueOnce(Promise.resolve());
            self.trigger('install');

            expect(self.skipWaiting).toHaveBeenCalled();
        });
    });
    describe('push', () => {
        it('should be able to show private chat text message notification', async () => {
            self.trigger('push', {
                data: { json: () => pushText },
            });

            await nextTick();

            const notifications = await self.registration.getNotifications();

            expect(notifications.length).toEqual(1);
            expect(notifications[0].title).toMatchSnapshot();
            expect(notifications[0].icon).toMatchSnapshot();
            expect(notifications[0].tag).toMatchSnapshot();
            expect(notifications[0].body).toMatchSnapshot();
        });
        it('should be able to show group chat text message notification', async () => {
            self.trigger('push', {
                data: { json: () => pushTextGroup },
            });

            await nextTick();

            const notifications = await self.registration.getNotifications();

            expect(notifications.length).toEqual(1);
            expect(notifications[0].title).toMatchSnapshot();
            expect(notifications[0].icon).toMatchSnapshot();
            expect(notifications[0].tag).toMatchSnapshot();
            expect(notifications[0].body).toMatchSnapshot();
        });
        it('should be able to show notification with mention', async () => {
            self.trigger('push', {
                data: { json: () => pushMention },
            });

            await nextTick();

            const notifications = await self.registration.getNotifications();

            expect(notifications.length).toEqual(1);
            expect(notifications[0].body).toMatchSnapshot();
        });
        it('should be able to show sticker notification', async () => {
            self.trigger('push', {
                data: { json: () => pushSticker },
            });

            await nextTick();

            const notifications = await self.registration.getNotifications();

            expect(notifications.length).toEqual(1);
            expect(notifications[0].body).toMatchSnapshot();
        });
        it('should be able to show file notification', async () => {
            self.trigger('push', {
                data: { json: () => pushFile },
            });

            await nextTick();

            const notifications = await self.registration.getNotifications();

            expect(notifications.length).toEqual(1);
            expect(notifications[0].body).toMatchSnapshot();
        });
        it('should be able to show reply notification', async () => {
            self.trigger('push', {
                data: { json: () => pushReply },
            });

            await nextTick();

            const notifications = await self.registration.getNotifications();

            expect(notifications.length).toEqual(1);
            expect(notifications[0].body).toMatchSnapshot();
        });
        it('should be able to show forward notification', async () => {
            self.trigger('push', {
                data: { json: () => pushForward },
            });

            await nextTick();

            const notifications = await self.registration.getNotifications();

            expect(notifications.length).toEqual(1);
            expect(notifications[0].body).toMatchSnapshot();
        });
        it('should be able to show not approved notification', async () => {
            self.trigger('push', {
                data: { json: () => pushNotApproved },
            });

            await nextTick();

            const notifications = await self.registration.getNotifications();

            expect(notifications.length).toEqual(1);
            expect(notifications[0].title).toMatchSnapshot();
            expect(notifications[0].body).toMatchSnapshot();
        });
    });
    describe('notificationClick', () => {
        it('should open chat on yandex.ru and set focus on tab if it does not exist', async () => {
            const clientsBefore = (await self.clients.matchAll()) as WindowClient[];
            expect(clientsBefore.length).toEqual(0);

            const notificationData: SW.NotificationOptionsData = {
                pushData: {
                    pushId: '123',
                    timestamp: new Date().getTime().toString(),
                },
                chatId: CHAT,
            };

            const notification = {
                close: jest.fn(),
                data: notificationData,
            };

            self.trigger('notificationclick', notification);
            const clients = (await self.clients.matchAll()) as WindowClient[];

            expect(notification.close).toHaveBeenCalled();
            expect(clients.length).toEqual(1);
            expect(clients[0].url).toEqual(`/chat#/chats/${encodeURIComponent(CHAT)}`);
        });
        it('should open private chat on yandex.ru and set focus on tab if it does not exist', async () => {
            const clientsBefore = (await self.clients.matchAll()) as WindowClient[];
            expect(clientsBefore.length).toEqual(0);

            const notificationData: SW.NotificationOptionsData = {
                pushData: {
                    pushId: '123',
                    timestamp: new Date().getTime().toString(),
                },
                chatId: PRIVATE_CHAT,
            };

            const notification = {
                close: jest.fn(),
                data: notificationData,
            };

            self.trigger('notificationclick', notification);
            const clients = (await self.clients.matchAll()) as WindowClient[];

            expect(notification.close).toHaveBeenCalled();
            expect(clients.length).toEqual(1);
            expect(clients[0].url).toEqual(`/chat#/chats/${encodeURIComponent(PRIVATE_CHAT)}`);
        });
        it('should open /chat and set focus on tab if it does not exist', async () => {
            const clientsBefore = (await self.clients.matchAll()) as WindowClient[];
            expect(clientsBefore.length).toEqual(0);

            const notificationData: SW.NotificationOptionsData = {
                pushData: {
                    pushId: '123',
                    timestamp: new Date().getTime().toString(),
                },
                chatId: FLOYD_CHAT,
            };

            const notification = {
                close: jest.fn(),
                data: notificationData,
            };

            self.trigger('notificationclick', notification);
            const clients = (await self.clients.matchAll()) as WindowClient[];

            expect(notification.close).toHaveBeenCalled();
            expect(clients.length).toEqual(1);
            expect(clients[0].url).toEqual(`/chat#/chats/${encodeURIComponent(FLOYD_CHAT)}`);
        });
        /** @todo https://st.yandex-team.ru/MSSNGRFRONT-2986
         * service-worker-mock не мокает frameType, нужно обойти это и раскомментировать тест.
         */
        // it('should focus on tab if it is opened and unfocused', async () => {
        //     await self.clients.openWindow(`${URL}#/chats/${encodeURIComponent(CHAT)}`);
        //
        //     const clientsBefore = (await self.clients.matchAll()) as WindowClient[];
        //     expect(clientsBefore[0].focused).toEqual(false);
        //
        //     const notification = {
        //         close: jest.fn(),
        //         data: { Chat: CHAT }
        //     };
        //
        //     self.trigger('notificationclick', notification);
        //     const clientsAfter = (await self.clients.matchAll()) as WindowClient[];
        //
        //     expect(notification.close).toHaveBeenCalled();
        //     expect(clientsAfter[0].focused).toEqual(true);
        // });
    });
});
