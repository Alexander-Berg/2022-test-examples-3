import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Контент тултипа.', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'отображается аватар пользователя': makeCase({
                id: 'marketfront-2566',
                issue: 'MARKETVERSTKA-29253',
                test() {
                    this.userProfilePopupSnippet.isAvatarVisible()
                        .should.eventually
                        .to.be.equal(true, 'Аватар пользователя отображается');
                },
            }),

            'отображается верный текст ссылки на отзывы': makeCase({
                id: 'marketfront-2619',
                issue: 'MARKETVERSTKA-29585',
                test() {
                    if (this.params.noReviews === true) {
                        return this.userProfilePopupSnippet.getNoReviewsText()
                            .should.eventually
                            .to.be.equal(this.params.reviewLink, 'Текст ссылки верный');
                    }

                    return this.userProfilePopupSnippet.getReviewLinkText()
                        .should.eventually
                        .to.be.equal(this.params.reviewLink, 'Текст ссылки верный');
                },
            }),

            'отображается блок экспертизы': makeCase({
                id: 'marketfront-2565',
                issue: 'MARKETVERSTKA-29252',
                test() {
                    return this.userProfilePopupSnippet.isExpertiseBlockVisible()
                        .should.eventually
                        .to.be.equal(
                            this.params.isExpertiseBlockVisible,
                            'Блок с экспертизой видно'
                        );
                },
            }),

            'содержит ссылку на публичную страницу всех отзывов': makeCase({
                id: 'marketfront-2563',
                issue: 'MARKETVERSTKA-29050',
                test() {
                    return Promise
                        .all([
                            this.userProfilePopupSnippet.getReviewLinkHref(),
                            this.browser.yaBuildURL('market:user-public', {
                                publicId: this.params.publicId,
                            }),
                        ])
                        .then(([currentLink, expectedLink]) =>
                            this.expect(currentLink).to.be.link(expectedLink, {
                                skipProtocol: true,
                                skipHostname: true,
                            })
                        );
                },
            }),
        },
    },
});
