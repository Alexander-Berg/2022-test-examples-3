import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ScrollBox} scrollBox
 */
export default makeSuite('Скроллбокс.', {
    story: {
        'Всегда': {
            'содержит ожидаемое количество элементов': makeCase({
                async test() {
                    const itemsCount = await this.scrollBox.getItemsCount();

                    await this.expect(itemsCount)
                        .to.be.equal(this.params.snippetsCount);
                },
            }),
        },
    },
});
