import {makeCase, makeSuite, mergeSuites} from 'ginny';

import PersonalCabinetProductHeadline from '@self/platform/components/PersonalCabinetProductHeadline/__pageObject';

export default makeSuite('Шапка сниппета в личном кабинете.', {
    params: {
        headerLink: 'Ссылка, куда ведет шапка',
    },
    story: mergeSuites({
        async beforeEach() {
            await this.setPageObjects({
                personalCabinetProductHeadline: () => this.createPageObject(PersonalCabinetProductHeadline),
            });
        },
        'По умолчанию': {
            'отображается': makeCase({
                id: 'marketfront-3844',
                async test() {
                    return this.personalCabinetProductHeadline.isVisible()
                        .should.eventually.be.equal(true, 'Шапка отображается');
                },
            }),
            'содержит корректную ссылку': makeCase({
                id: 'marketfront-3845',
                async test() {
                    const actualUrl = await this.personalCabinetProductHeadline.getLink();
                    const expectedUrl = this.params.headerLink;

                    return this.expect(actualUrl, 'Cсылка корректная')
                        .to.be.link(expectedUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
            'содержит картинку либо заглушку': makeCase({
                id: 'marketfront-3846',
                async test() {
                    return this.personalCabinetProductHeadline.isImageVisible()
                        .should.eventually.be.equal(true, 'Картинка товара отображается');
                },
            }),
        },
    }),
});
