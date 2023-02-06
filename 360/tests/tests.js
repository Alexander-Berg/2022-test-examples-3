mocha.setup('bdd');

/**
 * Функция для упрощенного создания тестов на операции.
 * Создает асинхронный тест, выполняет и заканчивает тест на определенные статусы операции.
 *
 * @param {Object} options
 * @param {string} options.desc Описание теста
 * @param {string} options.status Статус операции, на который надо выполнить проверку
 * @param {string} options.callback Функция теста, которыая выполнит проверку
 * @param {string} [options.statusTestDone] Статус на который надо закончить тест
 */
global.testOperation = function testOperation(options) {
    const func = function(done) {
        const test = sinon.spy(options.callback.bind(this));

        this.operation.on('status.' + options.status, test);

        this.operation.on('status.' + (options.statusTestDone || 'done'), () => {
            if (test.called) {
                done();
            } else {
                done('[`' + options.status + '`] Test have not called on operation status change.');
            }
        });

        this.operation.run();
    };

    func.toString = function() {
        return options.callback.toString();
    };

    it(options.desc, func);
};

require('../libs/extensions.noscript');

require('./request-animation-frame-fallback.js');
require('./xhr');
require('./ns-stubs');

window.Worker = null;

beforeEach(() => {
    // стабим обновление модели space
    // во многих операциях на status.done вызывается
    // ассинхронный fetch, который не обрабатывается fakeXHR,
    // т.к. сам тест на операцию завершен
    sinon.stub(ns.Model.get('space'), 'fetch', ns.nop);
});

afterEach(() => {
    ns.Model.get('space').fetch.restore();
    ns.Model._clear();
});

require('./spec/helperXiva');
require('./spec/helperElements');
require('./spec/routerEdit');
require('./spec/helperEditorDoc');

require('./spec/models/users-folder');
require('./spec/models/invitees-folder');
//require('./spec/models/state-invitee-folder');
require('./spec/models/contacts');

//require('./spec/ns.viewFetch');
//require('./spec/models/model-collection-lazy');
require('./spec/models/model-queue');
require('./spec/models/state-tree');
require('./spec/models/state-select-folder');

require('./spec/queues/queue-notifications');
require('./spec/queues/queue-sync');

require('./spec/operations/operations');
