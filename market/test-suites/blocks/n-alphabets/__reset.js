import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-alphabets.
 *
 * @param {PageObject.Alphabets} alphabets
 */
export default makeSuite('Блок с фильтром по алфавиту. Контрол для сброса всех фильтров', {
    feature: 'Фильтры',
    id: 'marketfront-833',
    issue: 'MARKETVERSTKA-24634',
    story: {
        'содержит ссылку на страницу списка всех брендов': makeCase({
            test() {
                return this.alphabets
                    .getResetFilterUrl()
                    .should.eventually.be.link({
                        pathname: '/brands',
                    }, {
                        skipProtocol: true,
                        skipHostname: true,
                    });
            },
        }),
    },
});
