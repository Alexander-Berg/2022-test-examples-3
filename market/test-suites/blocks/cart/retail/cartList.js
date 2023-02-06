import {makeCase, makeSuite} from 'ginny';

/**
 * Проверяет список офферов в корзине
 */
module.exports = makeSuite('Список офферов корзины', {
    environment: 'kadavr',

    params: {
        shouldShowBadge: 'Нужно ли показывать бейдж "Разобрали"',
    },

    story: {
        'Бейдж "Разобрали" отображается, если нужно': makeCase({
            async test() {
                return this.soldOutOverlay.isSoldOutVisible()
                    .should.eventually.to.be.equal(this.params.shouldShowBadge,
                        `Бейдж "разобрали" ${this.params.shouldShowBadge ? '' : 'не'} должен отображаться`);
            },
        }),
    },
});
