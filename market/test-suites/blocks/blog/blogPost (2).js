// flowlint-next-line untyped-import: off
import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Страница поста.', {
    feature: 'Функциональность',
    environment: 'kadavr',
    story: {
        'На странице': {
            'присутствует заголовок': makeCase({
                issue: 'MOBMARKET-12814',
                async test() {
                    const expectedTitle = this.params.title;
                    const titleElement = await this.blogPost.title;
                    return this.expect(titleElement.innerText).equal(
                        expectedTitle,
                        'Заголовок поста должен соответствовать ожиданиям'
                    );
                },
            }),

            'присутствует дата публикации': makeCase({
                issue: 'MOBMARKET-12814',
                async test() {
                    const expectedDate = this.params.firstPostPublishDate;
                    await this.browser.allure.runStep('Проверяем дату публикации первого поста', () =>
                        this.blogPost.publishDate
                            .getText()
                            .should.eventually.to.equal(expectedDate, 'Дата поста должна присутствовать')
                    );
                },
            }),

            'присутствует ссылка на список постов': makeCase({
                issue: 'MOBMARKET-12814',
                async test() {
                    const expectedUrl = await this.browser.yaBuildURL('touch:blog');
                    const href = await this.browser.allure.runStep('Проверяем ссылку на первый пост', () =>
                        this.blogPost.backNavigation.getAttribute('href')
                    );
                    return this.expect(href).to.be.link(expectedUrl, {skipProtocol: true, skipHostname: true});
                },
            }),
        },
    },
});
