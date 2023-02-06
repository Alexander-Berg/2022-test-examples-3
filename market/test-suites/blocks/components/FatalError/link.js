import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.FatalError} fatalError
 */
export default makeSuite('Ссылка сообщения.', {
    story: {
        'По умолчанию': {
            'ведёт на главную страницу': makeCase({
                test() {
                    return this.fatalError
                        .getActionButtonHref()
                        .then(currentUrl => this.expect(currentUrl).to.be.link(
                            {pathname: '/'},
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
