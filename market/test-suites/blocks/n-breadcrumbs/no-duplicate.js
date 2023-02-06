import _ from 'lodash';
import {props} from '@yandex-market/promise-helpers';
import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.Breadcrumbs} breadcrumbs
 */
export default makeSuite('Хлебные крошки (отсутствие дублирования).', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'нет дублирований.': makeCase({
                id: 'marketfront-2571',
                issue: 'MARKETVERSTKA-30265',
                async test() {
                    const linkNumbers = await this.breadcrumbs.getItemsCount();
                    const breadcrumbs = await Promise.all(_.times(
                        linkNumbers,
                        index => props({
                            title: this.breadcrumbs.getItemTextByIndex(index + 1),
                            url: this.breadcrumbs.getItemLinkByIndex(index + 1),
                        })
                    ));
                    return this.browser.allure.runStep('Проверяем на уникальность хлебные крошки', () => {
                        const uniqElements = _.uniqWith(breadcrumbs, _.isEqual).length;
                        return uniqElements.should.be.equal(
                            breadcrumbs.length,
                            'Хлебные крошки должны быть уникальными'
                        );
                    });
                },
            }),
        },
    },
});
