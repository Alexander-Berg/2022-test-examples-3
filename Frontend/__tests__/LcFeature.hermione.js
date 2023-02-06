hermione.skip.in(/searchapp|chrome-phone/, 'skipped by problem with scroll');
specs({
    feature: 'LcFeature',
}, () => {
    describe('Внешний вид блока. Размер L', function() {
        describe('Ориентация картинки', function() {
            hermione.only.notIn('safari13');
            it('1x1', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeL_ratio1x1.json')
                    .yaWaitForVisible(PO.lcSection(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcSection());
            });

            hermione.only.notIn('safari13');
            it('4x5', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeL_ratio4x5.json')
                    .yaWaitForVisible(PO.lcSection(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcSection());
            });

            hermione.only.notIn('safari13');
            it('16x9', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeL_ratio16x9.json')
                    .yaWaitForVisible(PO.lcSection(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcSection());
            });
        });

        describe('Наполнение контентом', function() {
            hermione.only.notIn('safari13');
            it('Фича с кнопкой', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeL_content.json')
                    .yaWaitForVisible(PO.lcFeatureWithButton(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcFeatureWithButton());
            });

            hermione.only.notIn('safari13');
            it('Фича со ссылкой', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeL_content.json')
                    .yaWaitForVisible(PO.lcFeatureWithLink(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcFeatureWithLink());
            });

            hermione.only.notIn('safari13');
            it('Фича с непрерывным текстом', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeL_content.json')
                    .yaWaitForVisible(PO.lcFeatureWithLongText(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcFeatureWithLongText());
            });

            hermione.only.notIn('safari13');
            it('Фича без описания', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeL_content.json')
                    .yaWaitForVisible(PO.lcFeatureWithoutSubtitle(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcFeatureWithoutSubtitle());
            });

            hermione.only.notIn('safari13');
            it('Фича без заголовка', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeL_content.json')
                    .yaWaitForVisible(PO.lcFeatureWithoutTitle(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcFeatureWithoutTitle());
            });

            hermione.only.notIn('safari13');
            it('Фича без кнопки / ссылки', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeL_content.json')
                    .yaWaitForVisible(PO.lcFeatureWithoutButton(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcFeatureWithoutButton());
            });

            hermione.only.notIn('safari13');
            it('Фича только с заголовком', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeL_content.json')
                    .yaWaitForVisible(PO.lcFeatureWithTitleOnly(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcFeatureWithTitleOnly());
            });

            hermione.only.notIn('safari13');
            it('Фича только с описанием', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeL_content.json')
                    .yaWaitForVisible(PO.lcFeatureWithSubtitleOnly(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcFeatureWithSubtitleOnly());
            });

            hermione.only.notIn('safari13');
            it('Фича с выравниванием по левому краю', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeL_content.json')
                    .yaWaitForVisible(PO.lcFeatureWithAlignLeft(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcFeatureWithAlignLeft());
            });
        });

        describe('Подложка', function() {
            hermione.only.notIn('safari13');
            it('Прямые углы с границей и заливкой', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeL_substrate.json')
                    .yaWaitForVisible(PO.lcFeatureSubstrateRightFull(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcFeatureSubstrateRightFull());
            });

            hermione.only.notIn('safari13');
            it('Прямые углы с границей без заливки', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeL_substrate.json')
                    .yaWaitForVisible(PO.lcFeatureSubstrateRightWithoutBackground(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcFeatureSubstrateRightWithoutBackground());
            });

            hermione.only.notIn('safari13');
            it('Прямые углы без границы с заливкой', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeL_substrate.json')
                    .yaWaitForVisible(PO.lcFeatureSubstrateRightWithoutBorder(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcFeatureSubstrateRightWithoutBorder());
            });

            hermione.only.notIn('safari13');
            it('Прямые углы без границы и без заливки', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeL_substrate.json')
                    .yaWaitForVisible(PO.lcFeatureSubstrateRightWithoutBackgroundBorder(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcFeatureSubstrateRightWithoutBackgroundBorder());
            });

            hermione.only.notIn('safari13');
            it('Закругленные углы с границей и заливкой', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeL_substrate.json')
                    .yaWaitForVisible(PO.lcFeatureSubstrateRoundFull(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcFeatureSubstrateRoundFull());
            });

            hermione.only.notIn('safari13');
            it('Закругленные углы с границей без заливки', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeL_substrate.json')
                    .yaWaitForVisible(PO.lcFeatureSubstrateRoundWithoutBackground(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcFeatureSubstrateRoundWithoutBackground());
            });

            hermione.only.notIn('safari13');
            it('Закругленные углы без границы с заливкой', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeL_substrate.json')
                    .yaWaitForVisible(PO.lcFeatureSubstrateRoundWithoutBorder(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcFeatureSubstrateRoundWithoutBorder());
            });

            hermione.only.notIn('safari13');
            it('Закругленные углы без границы и без заливки', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeL_substrate.json')
                    .yaWaitForVisible(PO.lcFeatureSubstrateRoundWithoutBackgroundBorder(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcFeatureSubstrateRoundWithoutBackgroundBorder());
            });
        });
    });

    describe('Внешний вид блока. Размер M', function() {
        describe('Ориентация картинки', function() {
            hermione.only.notIn('safari13');
            it('1x1', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeM_ratio1x1.json')
                    .yaWaitForVisible(PO.lcSection(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcSection());
            });

            hermione.only.notIn('safari13');
            it('4x5', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeM_ratio4x5.json')
                    .yaWaitForVisible(PO.lcSection(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcSection());
            });

            hermione.only.notIn('safari13');
            it('16x9', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeM_ratio16x9.json')
                    .yaWaitForVisible(PO.lcSection(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcSection());
            });
        });
    });

    describe('Внешний вид блока. Размер S', function() {
        describe('Ориентация картинки', function() {
            hermione.only.notIn('safari13');
            it('1x1', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeS_ratio1x1.json')
                    .yaWaitForVisible(PO.lcSection(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcSection());
            });

            hermione.only.notIn('safari13');
            it('4x5', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeS_ratio4x5.json')
                    .yaWaitForVisible(PO.lcSection(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcSection());
            });

            hermione.only.notIn('safari13');
            it('16x9', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeS_ratio16x9.json')
                    .yaWaitForVisible(PO.lcSection(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcSection());
            });
        });
    });

    describe('Внешний вид блока. Размер XS', function() {
        describe('Ориентация картинки', function() {
            hermione.only.notIn('safari13');
            it('1x1', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeXS_ratio1x1.json')
                    .yaWaitForVisible(PO.lcSection(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcSection());
            });

            hermione.only.notIn('safari13');
            it('4x5', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeXS_ratio4x5.json')
                    .yaWaitForVisible(PO.lcSection(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcSection());
            });

            hermione.only.notIn('safari13');
            it('16x9', function() {
                return this.browser
                    .url('/turbo?stub=lcfeature/sizeXS_ratio16x9.json')
                    .yaWaitForVisible(PO.lcSection(), 'Блок не загрузился')
                    .pause(200)
                    .yaScrollPageToBottom()
                    .assertView('plain', PO.lcSection());
            });
        });
    });
});
