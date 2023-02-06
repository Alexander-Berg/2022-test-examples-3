import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Бесконечная выдача', {
    story: {
        'Кнопка «показать ещё»': {
            'при появлении во вьюпорте должна подгружать следующую страницу': makeCase({
                id: 'm-touch-2472',
                issue: 'MOBMARKET-10336',
                async test() {
                    const initialCount = await this.searchResults.snippetsCount();
                    const location = await this.searchResults.showMore.getLocation();

                    await this.browser.scroll(location.x, location.y);

                    await this.browser.waitUntil(
                        async () => {
                            const snippetsCount = await this.searchResults.snippetsCount();

                            return (snippetsCount - initialCount) === initialCount;
                        },
                        15000,
                        'Не дождались загрузки сниппетов или загрузилось неправильное количество'
                    );
                },
            }),
            'не подгружает автоматически следующую страницу достигнув шестой': makeCase({
                id: 'm-touch-2473',
                issue: 'MOBMARKET-10337',
                async test() {
                    const getNextPage = async () => {
                        const location = await this.searchResults.showMore.getLocation();
                        const currentSnippetsCount = await this.searchResults.snippetsCount();

                        await this.browser.scroll(location.x, location.y);

                        await this.browser.waitUntil(
                            async () => {
                                const snippetsCount = await this.searchResults.snippetsCount();

                                return snippetsCount > currentSnippetsCount;
                            },
                            15000,
                            'Не дождались загрузки сниппетов или загрузилось неправильное количество'
                        );
                    };

                    await getNextPage();
                    await getNextPage();
                    await getNextPage();
                    await getNextPage();
                    await getNextPage();

                    const location = await this.searchResults.showMore.getLocation();

                    await this.browser.scroll(location.x, location.y);

                    const isButtonVisible = this.searchResults.showMore.isVisible();

                    return this.expect(isButtonVisible).to.be.equal(true, 'Кнопка присутствует');
                },
            }),
        },
        'Разделитель страниц': {
            'отрисовывается между второй и третьей': makeCase({
                id: 'm-touch-2474',
                issue: 'MOBMARKET-10338',
                async test() {
                    const getNextPage = async () => {
                        const location = await this.searchResults.showMore.getLocation();
                        const currentSnippetsCount = await this.searchResults.snippetsCount();

                        await this.browser.scroll(location.x, location.y);

                        await this.browser.waitUntil(
                            async () => {
                                const snippetsCount = await this.searchResults.snippetsCount();

                                return snippetsCount > currentSnippetsCount;
                            },
                            15000,
                            'Не дождались загрузки сниппетов или загрузилось неправильное количество'
                        );
                    };

                    await getNextPage();
                    await getNextPage();

                    const isVisible = this.searchResults.pageDelimiter.isVisible();

                    return this.expect(isVisible).to.be.equal(true, 'Разделитель присутствует');
                },
            }),
        },
        'Панель фильтров': {
            'должна быть видна при свайпе вверх': makeCase({
                id: 'm-touch-2475',
                issue: 'MOBMARKET-10339',
                async test() {
                    const initialCount = await this.searchResults.snippetsCount();
                    const location = await this.searchResults.showMore.getLocation();

                    await this.browser.scroll(location.x, location.y);

                    await this.browser.waitUntil(
                        async () => {
                            const snippetsCount = await this.searchResults.snippetsCount();

                            return (snippetsCount - initialCount) === initialCount;
                        },
                        15000,
                        'Не дождались загрузки сниппетов или загрузилось неправильное количество'
                    );

                    const moreButtonNewLocation = await this.searchResults.showMore.getLocation();

                    await this.browser.scroll(moreButtonNewLocation.x, moreButtonNewLocation.y);
                    await this.browser.scroll(moreButtonNewLocation.x / 2, moreButtonNewLocation.y / 2);

                    const isVisible = await this.searchResults.searchOptions.waitForVisible();

                    return this.expect(isVisible).to.be.equal(true, 'Панель фильтров видна');
                },
            }),
        },
    },
});
