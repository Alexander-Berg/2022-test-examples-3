import {makeSuite, makeCase} from 'ginny';

/**
 * Для отзыва от проверенного покупателя
 *
 * @param {PageObject.AuthorExpertise} authorExpertise
 */
export default makeSuite('Блок с экспертизой пользователя', {
    feature: 'Отображение отзыва',
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'должен отображаться': makeCase({
                id: 'marketfront-4171',
                issue: 'MARKETFRONT-14567',
                test() {
                    return this.authorExpertise.isVisible()
                        .should.eventually.to.be.equal(true, 'Отзыв должен показывать экпертность');
                },
            }),
        },
    },
});
