const isMimino = process.env.ENVIRONMENT_TYPE === 'mimino';
const passportHost = isMimino ? 'blackbox-mimino.yandex.net' : 'pass-test.yandex.ru';
const cloudAPIHost = isMimino ? 'api-mimino.dst.yandex.ru' : 'cloud-api.dst.yandex.ru';
const intapiHost = isMimino ? 'api-mimino.dst.yandex.net:8443' : 'api-stable.dst.yandex.net:8443';

module.exports = {
    ...require('./production'),
    blackbox: {
        host: passportHost,
        path: '/blackbox'
    },

    avatarsOrigin: 'https://avatars.mdst.yandex.net',

    chatWidgetLink: 'https://chat.s3.yandex.net/widget_light.js',

    cloudAPIHost,

    intapiHost,

    telemostHost: 'telemost.dst.yandex.ru',

    calendarHost: 'https://calendar.yandex.ru',

    vhLiveHost: 'vh.test.yandex.ru'
};
