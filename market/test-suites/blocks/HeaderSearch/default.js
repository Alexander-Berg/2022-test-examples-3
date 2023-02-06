import {
    makeSuite,
    mergeSuites,
    makeCase,
} from '@yandex-market/ginny';

export default makeSuite('Поисковое поле', {
    story: mergeSuites(
        {
            'В нужном контексте': {
                'имеет или не имеет чипсину': makeCase({
                    async test() {
                        const {
                            expectedWithChip,
                            expectedChipText,
                        } = this.params;

                        await this.headerSearch.isChipExisting().should.eventually.be.equal(
                            expectedWithChip,
                            'наличие чипсины соответствует ожидаемому'
                        );
                        await this.headerSearch.getChipText().should.eventually.be.equal(
                            expectedChipText,
                            'текст чипсины соответствует ожидаемому'
                        );
                    },
                }),
            },
        },
        {
            'По умолчанию': {
                'содержит плейсхолдер': makeCase({
                    async test() {
                        const {
                            expectedPlaceholder,
                        } = this.params;

                        await this.headerSearch.getPlaceholder().should.eventually.be.equal(
                            expectedPlaceholder,
                            'плейсхолдер поискового поля соответствует ожидаемому'
                        );
                    },
                }),
            },
        }
    ),
});
