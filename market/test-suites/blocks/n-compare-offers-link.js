import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок n-compare-offers-link.
 * @param {PageObject.CompareHead} compareHead
 * @param {PageObject.CompareRow} compareRow
 */
export default makeSuite('Ссылки "N предложений"', {
    environment: 'kadavr',
    feature: 'Сниппет ко/км',
    story: {
        'Ссылка "N предложений"': {
            'перекидывает на страницу "Цены" у данной карточки': makeCase({
                id: 'marketfront-183',
                issue: 'MARKETVERSTKA-24232',
                test() {
                    return this.compareRow.getOffersUrl()
                        .should.eventually.be.link({
                            pathname: 'product--.*/\\d+/offers',
                            query: {
                                track: 'fr_compare',
                            },
                        },
                        {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
    },
});
