import _ from 'lodash';
import {props} from '@yandex-market/promise-helpers';
import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.Breadcrumbs} breadcrumbs
 * @param {Array<string>} params.breadcrumbsText
 */
export default makeSuite('Хлебные крошки (присутствие на странице).', {
    environment: 'kadavr',
    params: {
        breadcrumbsText: 'Массив названий хлебных крошек',
    },
    story: {
        'По умолчанию': {
            'все присутствуют.': makeCase({
                id: 'marketfront-2570',
                issue: 'MARKETVERSTKA-30234',
                async test() {
                    const breadcrumbsItemsCount = await this.breadcrumbs.getItemsCount();

                    this.browser.allure.runStep(
                        'Проверяем что кол-во хлебных крошек совпадает с переданным кол-вом',
                        () => breadcrumbsItemsCount.should.be.equal(
                            this.params.breadcrumbsText.length,
                            'Кол-во хлебных крошек должно совпадать с переданным'
                        )
                    );

                    const breadcrumbs = await Promise.all(_.times(
                        breadcrumbsItemsCount,
                        index => props({
                            text: this.breadcrumbs.getItemTextByIndex(index + 1),
                        })
                    ));

                    return this.browser.allure.runStep('Проверяем хлебные крошки с переданными', () => {
                        breadcrumbs.forEach(({text}, index) => {
                            this.browser.allure.runStep(
                                `Проверяем что хлебная крошка «${text}» совпадает с «${this.params.breadcrumbsText[index]}», индекс: ${index}`,
                                () => {
                                    text.should.be.equal(
                                        this.params.breadcrumbsText[index],
                                        'Хлебные крошки должны совпадать с переданными'
                                    );
                                }
                            );
                        });
                    });
                },
            }),
        },
    },
});
