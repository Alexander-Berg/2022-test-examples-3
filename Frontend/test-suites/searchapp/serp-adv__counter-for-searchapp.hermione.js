'use strict';

const PO = require('../../page-objects/touch-phone/index').PO;
const hitCountUrl = 'bk848484(https://yabs.yandex.ru/count/hit_counter';
const countUrl = 'bk848484(https://yabs.yandex.by/count/';

function validCounter({ type, length, token }) {
    return {
        path: `/tech/direct/visibility/html/${token}/valid`,
        vars: {
            '-type': type,
            '-length': length,
            '-total_items': 4,
            '-total_link_tails': 3,
            '-bs_blockid': '*',
            '-reqid': '*'
        }
    };
}

specs({
    feature: 'Счетчик видимости рекламы',
    type: 'в мобильном приложении',
    experiment: 'текстом в html'
}, function() {
    it('Проверка наличия счетчиков hit_counter и link-head на странице', function() {
        return this.browser
            .yaOpenSerp({
                text: 'test',
                foreverdata: 3286532970
            })
            .yaCheckServerCounter(
                validCounter({ type: 'count', length: 221, token: 'counter' }),
                'Не сработал счетчик валидных данных'
            )
            .yaCheckServerCounter(
                validCounter({ type: 'hit_count', length: 40, token: 'hit_counter' }),
                'Не сработал счетчик валидных данных'
            )
            .yaShouldAllBeInvisible(PO.serpList.searchAppCounter())
            .yaExecute(function(counterSelector) {
                return document.querySelectorAll(counterSelector)[0].innerHTML;
            }, PO.serpList.searchAppCounter())
            .then(({ value }) => {
                assert.equal(value && value.includes(countUrl), true, 'Не найден счётчик link-head');
            })
            .yaExecute(function(counterSelector) {
                return document.querySelectorAll(counterSelector)[1].innerHTML;
            }, PO.serpList.searchAppCounter())
            .then(({ value }) => {
                assert.equal(value && value.includes(hitCountUrl), true, 'Не найден счётчик hit_counter');
            });
    });

    [{
        foreverdata: 2694445784,
        token: 'hit_counter'
    }, {
        foreverdata: 1298795869,
        token: 'counter'
    }].forEach(({ foreverdata, token }) => {
        it(`Проверка наличия счетчика невалидной ссылки ${token} на странице`, function() {
            return this.browser
                .yaOpenSerp({
                    text: 'окна',
                    foreverdata: foreverdata
                }, '.b-page')
                .yaCheckServerCounter({
                    path: `/tech/direct/visibility/html/${token}/invalid`,
                    vars: '-bs_blockid=*,-reqid=*'
                }, 'Не сработал счетчик невалидных данных');
        });
    });
});
