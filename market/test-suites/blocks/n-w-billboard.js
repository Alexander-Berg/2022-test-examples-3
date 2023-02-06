import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок n-w-billboard.
 * @param {PageObject.Billboard} billboard
 */
export default makeSuite('Блок Баннеры (Billboard).', {
    story: {
        'По умолчанию': {
            'должен присутствовать на странице': makeCase({
                id: 'm-touch-2427',
                issue: 'MOBMARKET-10117',
                test() {
                    return this.billboard
                        .isExisting()
                        .should.eventually.be.equal(true, 'Блок Баннеры присутствует на странице');
                },
            }),
        },
    },
});
