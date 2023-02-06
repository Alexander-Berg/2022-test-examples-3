import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок b-collection-item.
 * @param {PageObject.CollectionItem} collectionItem
 */
export default makeSuite('Блок продукт подборки', {
    story: {
        'Ссылка предложений': {
            'по умолчанию': {
                'должна вести на карточку товара': makeCase({
                    id: 'm-touch-1529',
                    issue: 'MOBMARKET-5910',

                    test() {
                        return (
                            this.collectionItem.aboutLink
                                .should.eventually.be.link({
                                    pathname: 'product--.*/\\d+',
                                },
                                {
                                    mode: 'match',
                                    skipProtocol: true,
                                    skipHostname: true,
                                })
                        );
                    },
                }),
            },
        },
    },
});
