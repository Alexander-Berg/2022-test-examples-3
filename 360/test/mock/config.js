export const defaultConfig = {
    UA: {},
    'url-prefix': '',
    'exp-test-ids': [],
    'is-corp': false,
    platform: 'ios',
    is669: false,
    device: 'phone',
    tz_offset: 0,
    thread_view: true,
    'yandex-domain': 'yandex.ru',
    locale: 'ru',
    version: 1,
    uid: '1112',
    _timestampDelta: 0,
    services: {},
    'corp-avatars-url': 'https://center.yandex-team.ru/api/v1/user/{{LOGIN}}/avatar/{{SIZE}}.jpg',
    connection_id: 42,
    maxDimension: 960,
    'mail-url': 'https://mail.yandex.uz',
    baseDir: '/touch',
    'docviewer-frontend-url': 'https://dv.test',
    inboxFid: '1',
    recommendedMessageCount: 12,
    'exp-boxes': '102710,0,20;149871,0,74',
    'eexp-boxes': '102710,0,20;149871,0,74',
    'send-message-url': 'do-send-url'
};

if (!window.__CONFIG) {
    window.__CONFIG = defaultConfig;
}

export default window.__CONFIG;
