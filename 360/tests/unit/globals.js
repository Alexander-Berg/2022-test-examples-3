const path = require('path');
const fs = require('fs');
const localize = require('@ps-int/d2-build-tools').localize;

const dump = localize.dump({
    locale: 'ru',
    path: [path.join(__dirname, '..', '..', 'loc')],
    namespace: '__i18n__'
});

/* eslint-disable no-eval */
eval('global.__i18n__ = ' + dump);
global.__i18n__.i18n = {};
eval(fs.readFileSync(path.join(__dirname, '..', '..', 'libs', 'i18n', 'convert.ru.js')).toString('utf-8'));
eval(fs.readFileSync(path.join(__dirname, '..', '..', 'libs', 'i18n', 'index.all.js')).toString('utf-8'));
/* eslint-enable no-eval */

const nop = () => {};

global.Vow = require('vow');

global.i18n = (tag, ...args) => {
    const loc = global.__i18n__[tag.substr(1)];
    return typeof loc === 'function' ? loc(...args) : loc;
};

global.ns = {
    page: {
        current: {
            params: {}
        }
    },
    events: {
        on: nop,
        off: nop,
        once: nop
    },
    log: {},
    Model: {
        define: nop,
        get: () => ({
            setDataDefault: () => {}
        }),
        prototype: {}
    },
    View: {
        prototype: {}
    },
    Update: {
        prototype: {}
    },
    router: {}
};

global.no = {
    inherit: () => {}
};

global.$ = () => ({
    text: () => '{}'
});

global.Ya = {
    Rum: {
        getTime: () => {}
    }
};

global.LANG = 'ru';
