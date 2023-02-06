import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на виджет Header.
 * @param {PageObject.Header} header
 */
export default makeSuite('Блок шапки.', {
    story: {
        'По-умолчанию': {
            'должен присутствовать на странице': makeCase({
                issue: 'MOBMARKET-6841',
                id: 'm-touch-1822',
                description: 'Проверяем появление шапки на главной странице',
                test() {
                    return this.header
                        .isExisting()
                        .should.eventually.to.be.equal(true, 'Шапка отобразилась');
                },
            }),
        },
    },
});
