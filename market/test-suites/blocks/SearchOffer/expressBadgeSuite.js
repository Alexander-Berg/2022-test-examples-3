import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на компонент ExpressBadge
 * @param {PageObject.ExpressBadge} badge
 */
export default makeSuite('Бейдж экспресса.', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'отображается': makeCase({
                async test() {
                    const isExisting = await this.expressBadge.isExisting();
                    return this.expect(isExisting).to.be.equal(true, 'Компонет бейджа экспресса присутствует');
                },
            }),
        },
    },
});
