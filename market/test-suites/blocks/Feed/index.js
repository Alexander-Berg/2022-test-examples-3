import {makeSuite, makeCase} from 'ginny';
import Feed from '@self/platform/spec/page-objects/Feed';

/**
 * Тесты на блок Feed
 * @param {PageObject.Feed} feed
 */
export default makeSuite('Виджет Фид.', {
    params: {
        snippetsCountBeforeLoading: 'Количество сниппетов до подгрузки',
        snippetsCountAfterLoading: 'Количество сниппетов после подгрузки',
    },
    story: {
        'По умолчанию': {
            'подгружает сниппеты по клику на кнопку "Показать еще"': makeCase({
                issue: 'MARKETVERSTKA-31511',
                id: 'marketfront-2926',
                async test() {
                    await this.browser.waitForVisible(Feed.loadMoreButton);

                    const beforeSnippetsCount = await this.feed.getSnippetsCount();

                    await this.expect(beforeSnippetsCount).to.equal(
                        this.params.snippetsCountBeforeLoading,
                        'Исходное количество сниппетов корректно'
                    );

                    await this.feed.loadMore();
                    await this.feed.waitUntilSnippetsCountChange(beforeSnippetsCount);

                    const snippetsCount = await this.feed.getSnippetsCount();

                    return this.expect(snippetsCount).to.equal(
                        this.params.snippetsCountAfterLoading,
                        'Сниппеты подгружаются корректно'
                    );
                },
            }),
        },
    },
});
