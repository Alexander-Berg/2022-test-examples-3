import {mergeSuites, makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок ClampLines.
 */
export default makeSuite('Компонент, схлопывающий длинный текст.', {
    feature: 'Оффер',
    environment: 'testing',
    story: mergeSuites(
        {
            'По умолчанию': {
                'должен правильно показывать текст': makeCase({
                    caseId: 'marketfront-2136',
                    params: {
                        fullText: 'Полный текст',
                        needToClamp: 'Должен ли компонент схлопывать текст',
                    },

                    test() {
                        if (this.params.needToClamp) {
                            return this.clampLines.hasMoreControl()
                                .should.eventually.be.equal(
                                    true,
                                    'Контрол "Ещё" должен выводиться'
                                )
                                .then(() => this.clampLines.clickMoreControl())
                                .then(() => this.clampLines.getVisibleText())
                                .should.eventually.be.equal(
                                    this.params.fullText,
                                    'Компонент должен отображать полный текст'
                                );
                        }

                        return this.clampLines.hasMoreControl()
                            .should.eventually.be.equal(
                                false,
                                'Контрол "Ещё" НЕ должен выводиться'
                            )
                            .then(() => this.clampLines.getVisibleText())
                            .should.eventually.be.equal(
                                this.params.fullText,
                                'Компонент должен отображать полный текст'
                            );
                    },
                }),
            },
        }
    ),
});
