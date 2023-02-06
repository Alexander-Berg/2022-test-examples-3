import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.SearchSnippet} snippet
 */
export default makeSuite('Список выдачи вида Grid.', {
    story: {
        'По умолчанию': {
            'должен содержать тайловые сниппеты': makeCase({
                id: 'm-touch-2027',
                issue: 'MOBMARKET-7813',
                test() {
                    return this.snippet
                        .isExisting()
                        .should.eventually.to.equal(true, 'Проверяем, что тайловые сниппеты присутствуют в списке');
                },
            }),
        },
    },
});
