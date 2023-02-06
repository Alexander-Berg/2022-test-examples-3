import {makeCase, makeSuite, mergeSuites} from 'ginny';

import ContentUserInfo from '@self/root/src/components/ContentUserInfo/__pageObject';

export default makeSuite('Блок информации об оставленном пользователем контенте.', {
    params: {
        userName: 'Имя пользователя',
    },
    story: mergeSuites({
        async beforeEach() {
            await this.setPageObjects({
                contentUserInfo: () => this.createPageObject(ContentUserInfo),
            });
        },
        'По умолчанию': {
            'отображается': makeCase({
                id: 'marketfront-3840',
                async test() {
                    return this.contentUserInfo.isVisible()
                        .should.eventually.be.equal(true, 'Блок отображается');
                },
            }),
            'содержит аватарку пользователя': makeCase({
                id: 'marketfront-3841',
                async test() {
                    return this.contentUserInfo.isAvatarVisible()
                        .should.eventually.be.equal(true, 'Аватарка пользователя отображается');
                },
            }),
            'содержит корректное имя пользователя': makeCase({
                id: 'marketfront-3842',
                async test() {
                    const expectedName = this.params.userName;
                    const actualName = this.contentUserInfo.getUserName();

                    return this.expect(actualName)
                        .to.be.equal(expectedName, 'Отображается корректное имя пользователя');
                },
            }),
            'содержит дату создания': makeCase({
                id: 'marketfront-3843',
                async test() {
                    return this.contentUserInfo.isCreationDateVisible()
                        .should.eventually.be.equal(true, 'Дата создания ответа отображается');
                },
            }),
        },
    }),
});
