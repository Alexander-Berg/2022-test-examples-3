import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Удаление основного товара', {
    feature: 'Акция товар + подарок',
    id: 'bluemarket-3125',
    story: {
        'При удалении основного товара': {
            async beforeEach() {
                await Promise.all([
                    this.primaryCartItem.waitForHidden(),
                    this.primaryCartItem.clickRemoveButton(),
                ]);
            },
            'должны стать удаленными основной товар и подарок': makeCase({
                async test() {
                    await this.primaryCartItem.isRemoved()
                        .should.eventually.be.equal(true,
                            'Не должен отображаться основной товар');

                    await this.giftCartItem.isRemoved()
                        .should.eventually.be.equal(true,
                            'Подарок должен стать удаленным');
                },
            }),
        },
    },
});
