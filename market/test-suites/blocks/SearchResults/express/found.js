import {makeSuite, makeCase} from '@yandex-market/ginny';

export default makeSuite('Поисковая выдача', {
    story: {
        'В листовой выдаче': {
            'сниппеты соответствуют ожидаемому': makeCase({
                async test() {
                    const {expectedExpressBadgeTexts} = this.params;
                    const expectedWithExpressBadge = Boolean(expectedExpressBadgeTexts);

                    await this.snippetCard.isExpressBadgeExisting()
                        .should.eventually.to.be.equal(
                            expectedWithExpressBadge,
                            'Наличие бейджа соответствует ожидаемому'
                        );
                    await this.snippetCard
                        .isExpressBadgeTextCorrect(expectedExpressBadgeTexts)
                        .should.eventually.to.be.equal(
                            expectedWithExpressBadge,
                            'Текст бейджа соответствует ожидаемому'
                        );
                },
            }),
        },
    },
});
