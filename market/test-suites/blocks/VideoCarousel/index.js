import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ScrollBox} scrollBox
 */
export default makeSuite('Карусель видеообзоров.', {
    feature: 'Карусель видеообзоров',
    story: {
        'По умолчанию': {
            'присутствует на странице': makeCase({
                id: 'm-touch-2536',
                issue: 'MOBMARKET-11042',
                test() {
                    return this.scrollBox
                        .isVisible()
                        .should.eventually.to.be.equal(true, 'Видеообзоры отображаются');
                },
            }),
            'скроллбокс скролится вправо до последнего элемента и назад': makeCase({
                id: 'm-touch-2539',
                issue: 'MOBMARKET-11045',
                async test() {
                    const lastItemPosition = await this.scrollBox.getItemsCount();

                    await this.scrollBox.scrollToElem(lastItemPosition).should.be.fulfilled;
                    await this.scrollBox.scrollToElem(1).should.be.fulfilled;
                },
            }),
        },
        'По клику на изображение видео': {
            'должна открыться страница видеообзоров.': makeCase({
                id: 'm-touch-2537',
                issue: 'MOBMARKET-11043',
                params: {
                    expectedUrl: 'ожидаемая ссылка на страницу видеообзоров',
                },
                async test() {
                    await this.browser.yaWaitForChangeUrl(() => this.scrollBox.clickOnItemByIndex(1));

                    const actualUrl = await this.browser.getUrl();
                    return this.browser.allure.runStep(
                        'Проверяем, что открылась страница видеообзора',
                        () => this.expect(actualUrl).to.be.link({
                            pathname: this.params.expectedUrl,
                        }, {
                            skipProtocol: true,
                            skipHostname: true,
                        })
                    );
                },
            }),
        },
    },
});
