import url from 'url';
import {makeSuite, makeCase} from 'ginny';
import Modal from '@self/platform/spec/page-objects/modal';

const testData = [
    {
        region: 'Киев',
        expectedTld: 'ua',
        testId: 'marketfront-929',
    },
    {
        region: 'Минск',
        expectedTld: 'by',
        testId: 'marketfront-930',
    },
    {
        region: 'Екатеринбург',
        expectedTld: 'ru',
        testId: 'marketfront-928',
    },
    {
        region: 'Астана',
        expectedTld: 'kz',
        testId: 'marketfront-927',
    },
];

export default makeSuite('Блок выбора региона.', {
    feature: 'Выбор региона',
    environment: 'testing',
    params: {
        withoutBem: false,
    },
    story: (() => {
        const suites = {
            beforeEach() {
                this.setPageObjects({
                    modal: () => this.createPageObject(Modal),
                });
            },
        };

        testData.forEach(test => {
            suites[`Изменение домена при смене региона на ${test.region}`] = makeCase({
                id: test.testId,
                test() {
                    const {host} = url.parse(this.browser.options.baseUrl);

                    return this.browser
                        .yaParseUrl()
                        .should.eventually.to.have.property('host', host, 'Проверяем, что хост соответствует базовому')
                        .yaScenario(this, 'region.changeTo', test.region)
                        .yaParseUrl()
                        .then(currentUrl => {
                            const splitHostName = currentUrl.hostname.split('.');

                            return splitHostName[splitHostName.length - 1];
                        })
                        .should.eventually.to.be.equal(test.expectedTld, 'Проверяем, что домен изменился');
                },
            });
        });

        return suites;
    })(),
});
