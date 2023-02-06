import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на элемент __logo блока header2.
 * @param {PageObject.Header2} header2
 */
export default makeSuite('Логотипы Я и Маркета.', {
    issue: 'MARKETVERSTKA-25697',
    story: {
        'Лого Яндекса': {
            'должнно иметь ссылку на Яндекс': makeCase({
                id: 'marketfront-1165',
                test() {
                    return this.header2.yandexLogo.getAttribute('href').should.be.eventually
                        .equal('https://yandex.ru/', 'Ссылка должна вести на главную Яндекса');
                },
            }),
        },

        'Лого Маркета': {
            'должно иметь ссылку на главную': makeCase({
                id: 'marketfront-1166',
                async test() {
                    const {hostname} = await this.browser.yaParseUrl();
                    return this.header2.marketLogo.getAttribute('href').should.be.eventually
                        .equal(`https://${hostname}/`, 'Ссылка должна вести на главную Маркета');
                },
            }),
        },
    },
});
