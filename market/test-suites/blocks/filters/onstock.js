import {makeSuite, makeCase} from 'ginny';
import _ from 'lodash';

/**
 * Тест на Фильтр «В продаже».
 * @param {PageObject.FilterCheckbox} filter
 * @param {PageObject.SnippetList} snippetList
 * @param {string} snippetCardPO
 */
export default makeSuite('Фильтр «В продаже»', {
    environment: 'kadavr',
    params: {
        snippetCardPO: 'Сниппет PageObject',
    },
    story: {
        'При активном фильтре': {
            'нет моделей, которые не в продаже': makeCase({
                id: 'marketfront-1957',
                issue: 'MARKETVERSTKA-27226',
                async test() {
                    const {snippetCardPO} = this.params;

                    await this.filter.isSelected().should.eventually.to.be.equal(true, 'Фильтр не выбран');

                    // Удостоверяемся в том, что загрузились только продукты в продаже
                    const snippetCards = await this.snippetList.getSnippetCardsLength(snippetCardPO)
                        .then(cardsLength => _.times(
                            cardsLength,
                            index => this.createPageObject(
                                snippetCardPO,
                                this.snippetList,
                                `${snippetCardPO.root}:nth-child(${index + 1})`
                            )
                        ));

                    return this.browser.allure.runStep(
                        'Проверяем, что все сниппеты в продаже',
                        async () => {
                            for (const snippetCard of snippetCards) {
                                // eslint-disable-next-line no-await-in-loop
                                await snippetCard.isOnStock()
                                    .should.eventually.to.be.equal(true, 'Сниппет в продаже');
                            }
                        }
                    );
                },
            }),
        },
    },
});
