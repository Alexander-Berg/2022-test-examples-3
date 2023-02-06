import {makeCase, makeSuite, mergeSuites} from 'ginny';

import PersonalCabinetZeroState from '@self/platform/components/PersonalCabinetZeroState/__pageObject';

export default makeSuite('Zero стейт страницы личного кабинета пользователя.', {
    story: mergeSuites({
        async beforeEach() {
            this.setPageObjects({
                zeroState: () => this.createPageObject(PersonalCabinetZeroState),
            });
        },
        'Если сниппеты отсутствуют': {
            'по умолчанию': {
                'отображается': makeCase({
                    id: 'marketfront-3833',
                    async test() {
                        return this.zeroState.isVisible()
                            .should.eventually.be.equal(true, 'Zero стейт страницы отображается');
                    },
                }),
            },
        },
    }),
});
