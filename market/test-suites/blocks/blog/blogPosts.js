import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Страница блога.', {
    environment: 'kadavr',
    feature: 'Функциональность',
    story: {
        'На странице': {
            'выводится n постов': makeCase({
                issue: 'MARKETVERSTKA-34863',
                id: 'marketfront-3661',
                test() {
                    return this.blogPosts.posts.then(posts => (
                        this.expect(posts.value.length)
                            .to.be.equal(
                                this.params.initialPostsCount,
                                `На странице должно быть ${this.params.initialPostsCount} постов`
                            )
                    ));
                },
            }),

            'внизу страницы': {
                'видна и работает кнопка "Показать еще"': makeCase({
                    issue: 'MARKETVERSTKA-34863',
                    id: 'marketfront-3663',
                    async test() {
                        await this.blogPosts.moreButton
                            .isVisible()
                            .should.eventually.to.be.equal(true, 'Кнопка "Показать еще" должна быть видна');

                        await this.browser.allure.runStep(
                            'Нажимаем на кнопку "Показать еще"',
                            () => this.blogPosts.moreButton.click()
                        );

                        await this.browser.allure.runStep(
                            'Ожидаем видимости первого догруженного поста',
                            () => this.blogPosts.waitForVisible(this.blogPosts.sixthBlogPost.getSelector)
                        );

                        return this.blogPosts.posts.then(posts => (
                            this.expect(posts.value.length)
                                .to.be.equal(
                                    this.params.initialPostsCount * 2,
                                    `На странице должно быть ${this.params.initialPostsCount * 2} постов`
                                )
                        ));
                    },
                }),
                'виден блок "Контактная информация"': makeCase({
                    issue: 'MARKETVERSTKA-34863',
                    id: 'marketfront-3662',
                    async test() {
                        await this.blogPosts.blogPrService
                            .isVisible()
                            .should.eventually.to.be.equal(true, 'Блок "Контактная информация" должен быть виден');
                    },
                }),
            },

            'при клике по ссылке "читать далее" работает переход на страницу поста': makeCase({
                issue: 'MARKETVERSTKA-34863',
                id: 'marketfront-3664',
                async test() {
                    const expectedPostPathname = await this.browser.yaBuildURL(
                        'market:blog-post',
                        {postSlug: this.params.posts[0].slug}
                    );

                    const postUrl = await this.browser.allure.runStep(
                        'Нажимаем на ссылку "читать далее" первого поста',
                        () => this.browser.yaWaitForChangeUrl(() => this.blogPosts.firstReadMoreLink.click())
                    );

                    await this.browser.allure.runStep(
                        'Открылась страница первого поста',
                        () => this.expect(postUrl).to.be.link({
                            pathname: expectedPostPathname,
                        }, {
                            mode: 'equal',
                            skipProtocol: true,
                            skipHostname: true,
                        })
                    );
                },
            }),

            'при клике по заголовку поста работает переход на страницу поста': makeCase({
                issue: 'MARKETVERSTKA-34863',
                id: 'marketfront-3665',
                async test() {
                    const expectedPostPathname = await this.browser.yaBuildURL(
                        'market:blog-post',
                        {postSlug: this.params.posts[0].slug}
                    );

                    const postUrl = await this.browser.allure.runStep(
                        'Нажимаем на заголовок первого поста',
                        () => this.browser.yaWaitForChangeUrl(() => this.blogPosts.firstTitle.click())
                    );

                    await this.browser.allure.runStep(
                        'Открылась страница первого поста',
                        () => this.expect(postUrl).to.be.link({
                            pathname: expectedPostPathname,
                        }, {
                            mode: 'equal',
                            skipProtocol: true,
                            skipHostname: true,
                        })
                    );
                },
            }),
        },

    },
});
