import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ColorFilter} colorFilter
 * @param {PageObject.Gallery} gallery
 */
export default makeSuite('Пикер цвета. Изменение фильтра.', {
    feature: 'Цвет на КМ',
    story: {
        'При выборе цвета.': {
            'Меняется основная картинка': makeCase({
                id: 'marketfront-466',
                issue: 'MARKETVERSTKA-33017',
                params: {
                    pickerIndexToSelect: 'Пикер, который надо выбрать',
                    expectedMainPicture: 'Ожидаемая основная картинка в Галерее',
                },
                async test() {
                    const {expectedMainPicture, selectedPickerIndex} = this.params;

                    if (this.params.skuState) {
                        await this.browser.setState('report', this.params.skuState);
                    }

                    await this.colorFilter.selectColor(selectedPickerIndex);
                    await this.browser.waitUntil(
                        async () => {
                            const updatedPicture = await this.gallery.getImageSrcFromElem(this.gallery.activeImage);

                            return updatedPicture === expectedMainPicture;
                        },
                        5000,
                        'основная картинка соответствует цвету'
                    );
                },
            }),

            'Меняются тамбы.': makeCase({
                id: 'marketfront-3275',
                issue: 'MARKETVERSTKA-33018',
                params: {
                    pickerIndexToSelect: 'Пикер, который надо выбрать',
                    expectedThumbs: 'Ожидаемые тамбы в Галерее',
                },
                async test() {
                    const {expectedThumbs, selectedPickerIndex} = this.params;

                    if (this.params.skuState) {
                        await this.browser.setState('report', this.params.skuState);
                    }

                    return this.colorFilter.selectColor(selectedPickerIndex)
                        .then(() => this.gallery.getThumbsSrc())
                        .should.eventually.have.same.members(expectedThumbs, 'тамбы соответствуют цвету');
                },
            }),
        },

        'Видимость крестика для сброса значения пикера': makeCase({
            id: 'marketfront-2052',
            params: {
                pickerIndexToSelect: 'Пикер, который надо выбрать',
                expectedResetButtonVisible: 'Ожидаемая видимость',
            },
            async test() {
                const {selectedPickerIndex, expectedResetButtonVisible = true} = this.params;

                if (this.params.skuState) {
                    await this.browser.setState('report', this.params.skuState);
                }

                return this.colorFilter.selectColor(selectedPickerIndex)
                    .then(() => this.colorFilter.resetButton.isVisible().catch(() => false))
                    .should.eventually.be.equal(expectedResetButtonVisible,
                        expectedResetButtonVisible
                            ? 'Крестик должен отображаться'
                            : 'Крестик не должен отображаться');
            },
        }),

        'Выбранный цвет отображается в пикере': makeCase({
            id: 'marketfront-2051',
            params: {
                pickerIndexToSelect: 'Пикер, который надо выбрать',
            },
            async test() {
                const {selectedPickerIndex} = this.params;

                if (this.params.skuState) {
                    await this.browser.setState('report', this.params.skuState);
                }

                const currentColor = await this.colorFilter.getColorByIndex(selectedPickerIndex);
                await this.colorFilter.selectColor(selectedPickerIndex);
                const colorName = await this.colorFilter.displayedValue.getText();

                return this.expect(colorName.toUpperCase())
                    .to.be.equal(currentColor.toUpperCase(), 'Цвета должны совпадать');
            },
        }),
    },
});
