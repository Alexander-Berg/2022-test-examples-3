import _ from 'lodash';
import ModelOperation from 'models/model-operation';
import ModelQueue from 'models/model-queue';

ModelOperation.prototype.nextTick = ModelQueue.prototype.nextTick = function(fn) {
    fn.call(this);
};

// абстрактная стаб операция, которая ничего не делает
ModelOperation.define('operationStub', {
    methods: {
        onStatusCreated: function() {
            this.setStatus('started');
        },
        onStatusStarted: function() {
            this.setStatus('progressing');
        },
        onStatusProgressing: function() {
            this.setStatus('done');
        },
        onStatusDone: no.nop
    }
});

ns.log.exception = function(type, exception) {
    throw exception;
};

const defaultsDeep = _.partialRight(_.merge, _.defaults);
// В страницу с тестами мы не подключаем реальный config и environment,
// вместо этого делаем минимальные фейковые.
const config = require('config');

defaultsDeep(config, {
    urlsBase: {
        public: 'https://yadi.sk',
        xiva: 'https://push.yandex.ru'
    },
    links: {
        statbox: 'clck.yandex.ru/jclck',
        static: {
            host: 'yastatic.net',
            libsRoot: 'https://' + location.host + '/static'
        }
    },
    environment: 'development'
});

const env = require('environment');

defaultsDeep(env, {
    agent: {
        isMobile: false
    },
    session: {
        locale: 'ru'
    }
});

require('models/user-current/user-current').setData({
    view: 'icons',
    foldersDefault: {
        photostream: 'disk/Фотокамера/',
        social: 'disk/Социальные сети/',
        yalivelettersarchive: '/attach/yalivelettersarchive',
        yaruarchive: '/attach/yaruarchive',
        yaslovariarchive: '/attach/yaslovariarchive',
        yateamnda: '/disk/Yandex Team (NDA)'
    }
});

ns.page.current.params = {
    idContext: '/test'
};
