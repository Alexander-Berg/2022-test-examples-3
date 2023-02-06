import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.ProductQuestionAnswerBackLink} backLink
 */
export default makeSuite('Блок "Вернуться на страницу вопроса".', {
    params: {
        backUrl: 'ссылка, которую ожидаем увидеть в компоненте перехода на предыдущую страницу',
    },
    story: {
        'По умолчанию': {
            'содержит корректную ссылку': makeCase({
                test() {
                    return this.backLink
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
});
