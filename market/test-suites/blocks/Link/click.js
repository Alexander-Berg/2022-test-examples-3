import {makeSuite, makeCase, mergeSuites} from 'ginny';

/**
 * Ссылка: тест на клик
 */
export default makeSuite('Ссылка.', {
    feature: 'Link',
    params: {
        selector: 'Селектор нажимаемой ссылки',
        pathname: 'Путь сслыки',
        pathParams: 'Параметры ссылки',
    },
    story: mergeSuites(
        {
            'По клику': {
                'страница должна смениться': makeCase({
                    async test() {
                        return this.browser
                            .yaWaitForChangeUrl(() => this.browser.click(this.params.selector), 10000)
                            .then(url => this.browser.yaBuildURL(this.params.pathname, this.params.pathParams)
                                .then(buildedUrl => this.expect(url)
                                    .to.be.link(buildedUrl, {
                                        mode: 'match',
                                        skipProtocol: true,
                                        skipHostname: true,
                                    })
                                ));
                    },
                }),
            },
        }
    ),
});
