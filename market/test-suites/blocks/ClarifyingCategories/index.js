import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ClarifyingCategories} clarifyingCategories
 */
export default makeSuite('Список уточнения категорий', {
    story: {
        'При клике на случайный пункт': {
            'должен происходить переход в категорию': makeCase({
                id: 'm-touch-2056',
                issue: 'MOBMARKET-7909',
                async test() {
                    await this.clarifyingCategories.getItemByIndex(1).click();
                    return this.browser
                        .getUrl()
                        .should.eventually.be.link({
                            pathname: '\\/catalog--[\\w-]+\\/[0-9]+\\/list',
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
    },
});
