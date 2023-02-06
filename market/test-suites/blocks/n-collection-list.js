import {mergeSuites, makeSuite, makeCase} from 'ginny';
import CollectionItem from '@self/platform/spec/page-objects/n-collection-item';

/**
 * Тесты на блок n-collection-list.
 * @param {PageObject.CollectionList} collectionList
 */
export default makeSuite('Список коллекции.', {
    feature: 'Статья',
    environment: 'testing',
    params: {
        collectionItemsCount: 'Количество элементов в коллекции',
    },
    story: mergeSuites(
        makeSuite('Добавление моделей в сравнение.', {
            story: {
                'По умолчанию': {
                    'имеет корректное количество товаров.': makeCase({
                        id: 'marketfront-1891',
                        test() {
                            // eslint-disable-next-line market/ginny/no-skip
                            return this.skip('MARKETVERSTKA-31797 скипаем упавшие тесты для озеленения');

                            // eslint-disable-next-line no-unreachable
                            const {collectionItemsCount} = this.params;

                            return this.collectionList.getItems()
                                .then(result => result.value.length)
                                .should.eventually.be.equal(collectionItemsCount, `В коллекции должно быть ${collectionItemsCount} товаров`);
                        },
                    }),
                },
                'При нажатии на все кнопки добавления в сравнения': {
                    'в сравнении получается корректное количество товаров.': makeCase({
                        id: 'marketfront-1891',
                        test() {
                            // eslint-disable-next-line market/ginny/no-skip
                            return this.skip('MARKETVERSTKA-31797 скипаем упавшие тесты для озеленения');

                            // eslint-disable-next-line no-unreachable
                            const {collectionItemsCount} = this.params;

                            return this.collectionList.getItemsCount()
                                .then(count => {
                                    const collectionItems = [];

                                    for (let i = 1; i <= count; i++) {
                                        collectionItems.push(this.createPageObject(
                                            CollectionItem,
                                            {
                                                parent: this.collectionList,
                                                root: `${CollectionItem.root}:nth-child(${i})`,
                                            }
                                        ));
                                    }
                                    return collectionItems;
                                })
                                .then(collectionItems => Promise.all(
                                    collectionItems.map(item => item.pushCompareButton()))
                                )
                                // Избавляемся от ресурса
                                // Здесь лучше проверить другим способом, что у нас есть в сравнении эти товары
                                //
                                // .then(() => Promise.all([
                                //     this.browser.yaUserStub(),
                                //     this.browser.yaRequestStub(),
                                // ])
                                //     .then(([user, request]) => this.browser.yaResource('comparison.get', {
                                //         user,
                                //         request,
                                //     }))
                                // )
                                .then(result => result.items[0].items.length)
                                .should.eventually.be.equal(collectionItemsCount, `В сравнении должно быть ${collectionItemsCount} товаров`);
                        },
                    }),
                },
            },
        })
    ),
});
