const config = require('config');

const tabs = [
    { url: '/', tabName: 'Похожие ссылки' },
    { url: '/memo', tabName: 'Добавлены в меморандум' },
];

describe('Авторизация в сервисе', function () {
    tabs.forEach((tab) => {
        it(`Незалогиненный в паспорте пользователь не может попасть на таб "${tab.tabName}"`, async function () {
            const { browser } = this;

            await browser.url(tab.url);

            const url = await browser.getUrl();

            assert.include(url, config.get('passport.host'), 'произошёл редирект на Паспорт');
        });
    });
});
