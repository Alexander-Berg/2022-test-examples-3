import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.widgets.content.HighratedSimilarProducts} highratedSimilarProducts
 */
export default makeSuite('Карусель "Пожожее с высоким рейтингом"', {
    feature: 'Структура страницы',
    params: {
        snippetsCount: 'Ожидаемое количество сниппетов в карусели',
    },
    story: {
        'По умолчанию': {
            'отображается корректно': makeCase({
                id: 'm-touch-3019',
                issue: 'MARKETFRONT-33504',
                async test() {
                    await this.browser.yaExecAsyncClientScript('window.initAllLazyWidgets');
                    // Делаем паузу, чтоб успели виджеты прогрузиться
                    // eslint-disable-next-line market/ginny/no-pause
                    await this.browser.pause(1000);
                    await this.highratedSimilarProducts.isVisible()
                        .should.eventually.to.be.equal(true, 'Карусель отображается');
                    await this.highratedSimilarProducts.getProductSnippetsCount()
                        .should.eventually.to.be.equal(this.params.snippetsCount, 'Количество сниппетов верное');
                },
            }),
        },
    },
});
