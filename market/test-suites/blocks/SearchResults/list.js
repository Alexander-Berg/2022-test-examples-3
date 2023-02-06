import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.SearchSnippet} snippet
 */
export default makeSuite('Список выдачи вида List.', {
    story: {
        'По умолчанию': {
            'должен содержать листовые сниппеты': makeCase({
                id: 'm-touch-2026',
                issue: 'MOBMARKET-7812',
                test() {
                    return this.snippet
                        .isExisting()
                        .should.eventually.to.equal(true, 'Проверяем, что листовые сниппеты присутствуют в списке');
                },
            }),
        },
    },
});
