import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Тег meta og:url', {
    feature: 'SEO',
    story: {
        'По умолчанию': {
            'должен соответствовать ожидаемому.': makeCase({
                params: {
                    expectedCanonicalLink: 'Ожидаемый Open Graph url',
                    leaveNoRedir: 'Не отрезать no-pda-redir=1',
                },
                async test() {
                    const {expectedCanonicalLink, leaveNoRedir} = this.params;

                    const url = await this.base
                        .getOpenGraphUrlContent()
                        .then(openGraphUrl => (leaveNoRedir ? openGraphUrl : openGraphUrl.replace(/[?|&]no-pda-redir=1/, '')));

                    await this.expect(url, 'Атрибут content соответствует переданному в параметрах')
                        .to.be.link(expectedCanonicalLink, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
    },
});
