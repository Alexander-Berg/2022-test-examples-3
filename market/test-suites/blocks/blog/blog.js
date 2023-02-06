// flowlint-next-line untyped-import: off
import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Список постов.', {
    feature: 'Функциональность',
    environment: 'kadavr',
    story: {
        'На странице': {
            'присутствует заголовок "Новости"': makeCase({
                issue: 'MOBMARKET-12814',
                async test() {
                    const expectedTitle = this.params.title;
                    const title = await this.blog.title;
                    return this.expect(title.innerText).equal(expectedTitle, 'Заголовок должен присутствовать');
                },
            }),

            'присутствует заголовок первого поста': makeCase({
                issue: 'MOBMARKET-12814',
                async test() {
                    const expectedPostTitle = this.params.firstPost.title;
                    await this.browser.allure.runStep('Проверяем заголовок первого поста', () =>
                        this.blog
                            .getNthPostTitle(1)
                            .getText()
                            .should.eventually.to.equal(
                                expectedPostTitle,
                                'Заголовок первого поста должен соответствовать ожиданиям'
                            )
                    );
                },
            }),

            'присутствует ссылка на первый пост': makeCase({
                issue: 'MOBMARKET-12814',
                async test() {
                    const {slug} = this.params.firstPost;
                    const expectedUrl = await this.browser.yaBuildURL('touch:blog-post', {postSlug: slug});
                    const href = await this.browser.allure.runStep('Проверяем ссылку на первый пост', () =>
                        this.blog.getNthPostLink(1).getAttribute('href')
                    );
                    return this.expect(href).to.be.link(expectedUrl, {skipProtocol: true, skipHostname: true});
                },
            }),

            'присутствует дата публикации первого поста': makeCase({
                issue: 'MOBMARKET-12814',
                async test() {
                    const expectedDate = this.params.firstPost.date;
                    await this.browser.allure.runStep('Проверяем дату публикации первого поста', () =>
                        this.blog
                            .getNthPostDate(1)
                            .getText()
                            .should.eventually.to.equal(expectedDate, 'Дата поста должна присутствовать')
                    );
                },
            }),

            'отображается кнопка "показать еще"': makeCase({
                issue: 'MOBMARKET-12814',
                async test() {
                    const isVisible = await this.browser.allure.runStep(
                        'Проверяем, наличие кнопки "Показать еще"',
                        () => this.blog.loadMoreButton.isVisible().catch(() => false)
                    );
                    return this.expect(isVisible).equal(true, 'Кнопка "показать еще" отображается');
                },
            }),
        },
    },
});
