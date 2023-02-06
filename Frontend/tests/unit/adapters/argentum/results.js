/**
 * Тесты для модуля работы с Аргентумом
 */

const proxyquire = require('proxyquire');
const rp = require('../../fixtures/request-promise');
const Logger = require('../../fixtures/logger');

const Argentum = proxyquire.load('../../../../src/server/adapters/argentum/results', {
    'request-promise': rp,
});

// TODO разобраться как мокать rp Argentum, от которого наследуется ArgentumResults
describe.skip('ArgentumResults', () => {
    const host = 'https://dev.argentum.yandex-team.ru';
    const logger = new Logger();

    let argentum;

    const experimentId = 1;

    beforeEach(() => {
        argentum = new Argentum(host, logger);
    });

    afterEach(() => {
        rp.resetBehavior();
        rp.reset();
    });

    describe('getWinsAgainstControlSystem', () => {
        it('должен делать запрос с передачей фильтра по тегам, если он был в параметрах', function() {
            const params = {
                leftSystemId: '0',
                rightSystemId: '1',
                controlSystemId: '2',
                metrics: ['win-rate', 'bt'],
                tagsFiltering: 'ewogICJleHQtb3AiOiAib3IiLAogICJmaWx0ZXJzIjogWwogICAgewogICAgICAic3lzdGVtIjog==',
            };

            return argentum.getWinsAgainstControlSystem(experimentId, params)
                .then(() => {
                    assert.isTrue(rp.calledOnce);
                    assert.calledWith(rp, {
                        resolveWiThFullResponse: true,
                        uri: 'https://dev.argentum.yandex-team.ru/api/1/results/sbs/wins-against-control-system/v2',
                        method: 'GET',
                        qsStringifyOptions: { arrayFormat: 'repeat' },
                        json: true,
                        qs: {
                            'left-system-id': '0',
                            'right-system-id': '1',
                            'control-system-id': '2',
                            metric: ['win-rate', 'bt'],
                            'tags-filtering': 'ewogICJleHQtb3AiOiAib3IiLAogICJmaWx0ZXJzIjogWwogICAgewogICAgICAic3lzdGVtIjog==',
                        },
                    });
                });
        });
    });
});
