import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Тег Open Graph url', {
    feature: 'SEO',
    story: {
        'По умолчанию': {
            'должен соответствовать ожидаемому.': makeCase({
                id: 'm-touch-1041',
                issue: 'MOBMARKET-6319',
                params: {
                    expectedCanonicalLink: 'Ожидаемый Open Graph url',
                    leaveNoRedir: 'Не отрезать no-pda-redir=1',
                },
                test() {
                    const {expectedCanonicalLink, leaveNoRedir} = this.params;

                    return this.base
                        .getOpenGraphUrlContent()
                        .then(url => (leaveNoRedir ? url : url.replace(/[?|&]no-pda-redir=1/, '')))
                        .should.eventually.to.equal(expectedCanonicalLink, 'Проверяем Open Graph url');
                },
            }),
        },
    },
});
