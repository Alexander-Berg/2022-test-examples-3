import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-alphabets.
 *
 * @param {PageObject.Alphabets} alphabets
 */
export default makeSuite('Блок с фильтром по алфавиту. Контрол с фильтром по цифрам.', {
    feature: 'Фильтры',
    id: 'marketfront-834',
    issue: 'MARKETVERSTKA-24633',
    story: {
        'Если не активирован': {
            'содержит ссылку на страницу с фильтром по цифрам': makeCase({
                test() {
                    return this.alphabets
                        .getNumFilterUrl()
                        .should.eventually.be.link({
                            pathname: '/brands',
                            query: {
                                char: 'num',
                            },
                        }, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
    },
});
