import {makeSuite, makeCase} from 'ginny';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Попап аналогов.', {
    environment: 'kadavr',
    params: {},
    story: {
        'у товара не в наличии отображается кнопка аналогов': makeCase({
            id: 'MARKETYAPROJECT-158',
            issue: 'MARKETYAPROJECT-158',

            async test() {
                await this.unknownSnippet.openSimilarPopup();

                await this.similarPopup.waitForVisibleRoot();
            },
        }),
    },
});
