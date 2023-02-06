import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.SubpageHeader} subpageHeader
 */
export default makeSuite('Заголовок страницы.', {
    params: {
        backUrl: 'ссылка, которая находится в компоненте перехода на предыдущую страницу',
    },
    story: {
        'Стрелка назад.': {
            'По умолчанию': {
                'содержит корректную ссылку': makeCase({
                    test() {
                        return this.subpageHeader
                            .getBackLinkHref()
                            .then(currentUrl => this.expect(currentUrl).to.be.link(
                                {pathname: this.params.backUrl},
                                {
                                    skipProtocol: true,
                                    skipHostname: true,
                                }
                            ));
                    },
                }),
            },
        },
    },
});
