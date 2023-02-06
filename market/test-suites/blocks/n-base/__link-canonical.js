import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Тег link canonical', {
    feature: 'SEO',
    story: {
        'По умолчанию': {
            'должен соответствовать ожидаемому.': makeCase({
                params: {
                    expectedCanonicalLink: 'Ожидаемый link canonical',
                    leaveNoRedir: 'Не отрезать no-pda-redir=1',
                },
                async test() {
                    const {expectedCanonicalLink, leaveNoRedir} = this.params;

                    const url = await this.base
                        .getCanonicalLinkContent()
                        .then(canonicalUrl => (leaveNoRedir ? canonicalUrl : canonicalUrl.replace(/[?|&]no-pda-redir=1/, '')));

                    await this.expect(url, 'Атрибут href соответствует переданному в параметрах')
                        .to.be.link(expectedCanonicalLink, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
    },
});
