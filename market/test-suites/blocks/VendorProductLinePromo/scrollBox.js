import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ScrollBox} scrollBox
 */
export default makeSuite('Скроллбокс.', {
    story: {
        'Всегда': {
            'содержит ожидаемое количество элементов': makeCase({
                async test() {
                    const items = await this.scrollBox.getItems();

                    await this.expect(items.value.length)
                        .to.be.equal(this.params.snippetsCount);
                },
            }),
        },
    },
});
