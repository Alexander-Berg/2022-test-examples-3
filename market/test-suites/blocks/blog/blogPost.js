import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Страница поста.', {
    environment: 'kadavr',
    feature: 'Функциональность',
    story: {
        'На странице': {
            'работает переход на страницу блога по ссылке "Все новости"': makeCase({
                issue: 'MARKETVERSTKA-34863',
                id: 'marketfront-3666',
                async test() {
                    const expectedBlogPathname = await this.browser.yaBuildURL('market:blog');

                    const blogUrl = await this.browser.allure.runStep(
                        'Нажимаем на ссылку "Все новости"',
                        () => this.browser.yaWaitForChangeUrl(() => this.blogPost.blogLink.click())
                    );

                    await this.browser.allure.runStep(
                        'Ожидаем видимости страницы',
                        () => this.blogPosts.waitForVisible()
                    );

                    await this.browser.allure.runStep(
                        'Открылась страница блога',
                        () => this.expect(blogUrl).to.be.link({
                            pathname: expectedBlogPathname,
                        }, {
                            mode: 'equal',
                            skipProtocol: true,
                            skipHostname: true,
                        })
                    );
                },
            }),

            'присутствуют заголовок и дата публикации': makeCase({
                issue: 'MARKETVERSTKA-34863',
                id: 'marketfront-3667',
                async test() {
                    const expectedPostTitle = this.params.firstPostTitle;

                    await this.blogPost.postTitle
                        .getText()
                        .should.eventually.to.be.equal(expectedPostTitle, 'Заголовок поста должен присутствовать');

                    const expectedPostPublishDate = this.params.firstPostPublishDate;

                    await this.blogPost.postPublishDate
                        .getText()
                        .should.eventually.to.be.equal(expectedPostPublishDate, 'Дата поста должна присутствовать');
                },
            }),
        },

    },
});
