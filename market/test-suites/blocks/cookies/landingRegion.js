import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Cookie lr.', {
    feature: 'Регион',
    environment: 'testing',
    story: {
        'По умолчанию': {
            'должна содержать код региона': makeCase({
                id: 'marketfront-1029',
                issue: 'MARKETVERSTKA-25090',
                async test() {
                    const cookie = await this.browser.getCookie('lr');
                    return this.expect(cookie.value).to.be.equal('54');
                },
            }),
        },
    },
});
