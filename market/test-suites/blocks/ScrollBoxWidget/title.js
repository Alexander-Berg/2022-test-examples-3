import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ScrollBoxWidget} scrollBox
 */
export default makeSuite('Виджет ScrollBox.', {
    story: {
        'Всегда': {
            'содержит ожидаемый заголовок': makeCase({
                async test() {
                    const titleText = await this.scrollBoxWidget.getTitleText();
                    await this.expect(titleText).to.be.equal(this.params.titleText);
                },
            }),
        },
    },
});
