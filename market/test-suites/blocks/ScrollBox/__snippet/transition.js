import {makeSuite, makeCase} from 'ginny';

export default makeSuite('ScrollBox переходы.', {
    feature: 'ScrollBox',
    environment: 'testing',
    story: {
        'При клике на спиппет': {
            'должен произойти переход на другую страницу.': makeCase({
                id: 'm-touch-1934',
                async test() {
                    await this.ScrollBox.getItemByIndex(1).click();
                    return this.browser
                        .getUrl()
                        .should.eventually.be.link({
                            pathname: this.params.pathname,
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
    },
});
