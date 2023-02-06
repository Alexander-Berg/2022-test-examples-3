import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.Breadcrumbs} breadcrumbs
 */
export default makeSuite('Хлебные крошки (последняя некликабельная).', {
    environment: 'kadavr',
    story: {
        'Последняя крошка': {
            'По условию': {
                'является некликабельной.': makeCase({
                    id: 'marketfront-2574',
                    issue: 'MARKETVERSTKA-30235',
                    async test() {
                        const hasLink = await this.breadcrumbs.hasLastItemLink();

                        return this.browser.allure.runStep(
                            'Проверяем что последняя хлебная крошка некликабельная',
                            () => hasLink.should.be.equal(
                                true,
                                'Последняя хлебная крошка некликабельна'
                            )
                        );
                    },
                }),
            },
        },
    },
});
