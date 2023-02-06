module.exports = {

    languageCaptions: {
        ru: 'Русский',
        uk: 'Українська',
        be: 'Беларуская',
        kk: 'Қазақ',
        tr: 'Türkçe',
        en: 'English'
    },

    languages: {
        ru: {
            langs: [ 'ru', 'uk', 'en', 'be', 'kk' ]
        },
        ua: {
            langs: [ 'ru', 'uk', 'en', 'be', 'kk' ]
        }
    },

    solomon: {
        backend: {
            host: 'someHost',
            port: 'somePort',
            path: 'somePath'
        },
        commonLabels: {
            project: 'someProject',
            service: 'someService',
            host: 'someHost'
        },
        wrapDebugTimers: true,
    }
};
