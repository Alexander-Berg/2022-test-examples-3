jest.mock('../../components/extract-preloaded-data', () => () => ({
    userCurrent: { sids: [], states: {} },
    settings: {},
    environment: {
        agent: {
            isMobile: false,
            OSFamily: 'Linux'
        },
        session: {
            locale: 'ru',
            experiment: {}
        }
    },
    config: {
        urlsBase: {
            disk: 'https://disk.yandex.ru',
            public: 'https://yadi.sk',
            xiva: 'https://push.yandex.ru',
            support: 'https://yandex.ru/support'
        },
        environment: 'development',
        data: {
            url: 'https://disk.yandex.ru',
            tld: 'ru'
        }
    },
    defaultFolders: {
        folders: {
            photostream: '/disk/Фотокамера',
            social: '/disk/Социальные сети',
            yalivelettersarchive: '/attach/yalivelettersarchive',
            yaruarchive: '/attach/yaruarchive',
            yaslovariarchive: '/attach/yaslovariarchive',
            yateamnda: '/disk/Yandex Team (NDA)'
        }
    }
}));

// @ts-ignore
global.$ = () => ({
    text: () => '{}'
});

// @ts-ignore
global.$.ajax = (...args) => {
    console.log('ajax:', args); // eslint-disable-line
};

require('@ps-int/ufo-noscript/dist/noscript');
require('../../libs/extensions.noscript');
