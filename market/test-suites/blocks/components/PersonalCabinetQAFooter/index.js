import {makeCase, makeSuite, mergeSuites} from 'ginny';

import PersonalCabinetQAFooter from '@self/platform/components/PersonalCabinetQAFooter/__pageObject';
import Votes from '@self/platform/spec/page-objects/components/Votes';

export default makeSuite('Футер сниппета в личном кабинете.', {
    params: {
        likesCount: 'Количество лайков сниппета',
        footerLink: 'Url ссылки, на которую ведет футер',
        linkText: 'Текст ссылки',
    },
    story: mergeSuites({
        async beforeEach() {
            await this.setPageObjects({
                footer: () => this.createPageObject(PersonalCabinetQAFooter),
                votes: () => this.createPageObject(Votes),
            });
        },
        'По умолчанию': {
            'отображается': makeCase({
                id: 'marketfront-3847',
                async test() {
                    return this.footer.isVisible()
                        .should.eventually.be.equal(true, 'Футер отображается');
                },
            }),
            'содержит корректный текст ссылки': makeCase({
                id: 'marketfront-3848',
                async test() {
                    const expectedText = this.params.linkText;

                    return this.footer.getLinkText()
                        .should.eventually.be.equal(expectedText, 'Текст ссылки корректный');
                },
            }),
            'содержит корректную ссылку': makeCase({
                id: 'marketfront-3849',
                async test() {
                    const expectedUrl = this.params.footerLink;
                    const actualUrl = this.footer.getLink();

                    return this.expect(actualUrl, 'Cсылка корректная')
                        .to.be.link(expectedUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
        'Голосовалка.': {
            'По умолчанию': {
                'отображает верное количество лайков': makeCase({
                    id: 'marketfront-3850',
                    async test() {
                        const expectedLikesCount = this.params.likesCount;

                        return this.votes.getLikeCount()
                            .should.eventually.be.equal(expectedLikesCount, 'Количество лайков верное');
                    },
                }),
            },
            'При проставлении лайка': {
                'количество лайков увеличивается': makeCase({
                    id: 'marketfront-3851',
                    async test() {
                        const currentLikeCount = await this.votes.getLikeCount();
                        const expectedLikeCount = currentLikeCount + 1;

                        await this.browser.yaWaitForChangeValue({
                            action: () => this.votes.clickLike(),
                            valueGetter: () => this.votes.getLikeCount(),
                        });

                        const nextLikeCount = await this.votes.getLikeCount();
                        await this.expect(nextLikeCount)
                            .to.equal(expectedLikeCount, 'Количество лайков увеличилось');

                        await this.browser.refresh();

                        return this.votes.getLikeCount()
                            .should.eventually.be.equal(
                                expectedLikeCount,
                                'Количество лайков при рефреше не измениолось');
                    },
                }),
            },
            'При клике на уже проставленный лайк': {
                'каунтер уменьшается на 1': makeCase({
                    id: 'marketfront-3852',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.votes.clickLike(),
                            valueGetter: () => this.votes.getLikeCount(),
                        });

                        const currentLikeCount = await this.votes.getLikeCount();
                        const expectedLikeCount = currentLikeCount - 1;

                        await this.browser.yaWaitForChangeValue({
                            action: () => this.votes.clickLike(),
                            valueGetter: () => this.votes.getLikeCount(),
                        });

                        const nextLikeCount = await this.votes.getLikeCount();

                        return this.expect(nextLikeCount)
                            .to.equal(expectedLikeCount, 'Количество лайков уменьшилось');
                    },
                }),
            },
        },
    }),
});
