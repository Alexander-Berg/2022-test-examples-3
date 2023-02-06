import { WebSocketTransport } from '@yandex-int/messenger.websocket';
import { Uniproxy } from '@yandex-int/messenger.uniproxy';

import { UniproxySpeechkitService } from '../src/UniproxySpeechkitService';
import { UniproxyConfig } from '../src/UniproxyConfig';
import { Speechkit } from '../src/Speechkit';
import { vinsRequestFactoryProvider } from '../src/Vins/vinsRequestFactory';

// Не использовать apiKey, url, appId мессенджера в продакшене!!!
const config = new UniproxyConfig({
    apiKey: '069b6659-984b-4c5f-880e-aaedcfd84102',
    url: 'wss://uniproxy.messenger.yandex.ru/uni.ws',
    suspendable: true,
});

const websocket = new WebSocketTransport();

websocket.setup(config.getUrl());

const uniproxy = new Uniproxy(websocket);
const uniproxySpeechkitService = new UniproxySpeechkitService(uniproxy, config);

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const speechkit = new Speechkit(
    uniproxySpeechkitService,
    vinsRequestFactoryProvider({
        uuid: 'some_test_uuid2',
        appId: 'ru.yandex.messenger',
        appVersion: '1.0.1',
        platform: navigator.platform,
        osVestion: navigator.userAgent,
        vinsTopic: 'desktopgeneral',
        experiments: ['multi_tabs', 'find_poi_gallery', 'find_poi_one'],
        timezone: 'Europe/Moscow',
    }),
);

(window as any).__speechkit = speechkit;
