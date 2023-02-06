import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ScrollBoxWidget} scrollBoxWidget
 */
export default makeSuite('Виджет ScrollBox.', {
    story: {
        'Всегда': {
            'содержит ожидаемый заголовок': makeCase({
                params: {
                    titleText: 'Текст заголовка',
                },
                async test() {
                    const titleText = await this.scrollBoxWidget.getTitleText();
                    await this.expect(titleText).to.be.equal(this.params.titleText);
                },
            }),
        },
    },
});
