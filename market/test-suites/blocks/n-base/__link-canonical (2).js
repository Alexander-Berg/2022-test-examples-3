import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Тег link canonical.', {
    feature: 'SEO',
    story: {
        'По умолчанию': {
            'должен соответствовать ожидаемому.': makeCase({
                id: 'm-touch-1040',
                issue: 'MOBMARKET-6323',
                params: {
                    expectedCanonicalLink: 'Ожидаемый link canonical',
                    leaveNoRedir: 'Не отрезать no-pda-redir=1',
                },
                test() {
                    const {expectedCanonicalLink, leaveNoRedir} = this.params;

                    return this.base
                        .getCanonicalLinkContent()
                        .then(url => (leaveNoRedir ? url : url.replace(/[?|&]no-pda-redir=1/, '')))
                        .should.eventually.to.equal(expectedCanonicalLink, 'Проверяем link canonical');
                },
            }),
        },
    },
});
