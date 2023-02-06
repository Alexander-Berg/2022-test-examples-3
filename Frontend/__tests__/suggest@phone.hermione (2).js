/* eslint @typescript-eslint/no-use-before-define: 0 */

describe('Ecom-tap', () => {
    describe('Поисковый саджест', () => {
        it('Внешний вид', async function() {
            const browser = this.browser;

            await mockSuggestResponse(browser, response());

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
                expFlags: { 'turbo-app-ecom-search-suggest': 1, 'turbo-app-global-search': 1 },
                query: { query: '1' },
            });

            await browser.yaWaitForVisible('.mini-suggest__input');
            await browser.click('.mini-suggest__input');
            await browser.yaWaitForVisible('.mini-suggest__popup');
            await browser.assertView(
                'suggest',
                ['.Navigation', '.mini-suggest__popup-content']
            );
        });

        it('Клик в товар', async function() {
            const browser = this.browser;

            await mockSuggestResponse(browser, response());

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
                expFlags: { 'turbo-app-ecom-search-suggest': 1, 'turbo-app-global-search': 1 },
                query: { query: '1' },
            });

            await browser.yaWaitForVisible('.mini-suggest__input');
            await browser.click('.mini-suggest__input');
            await browser.yaWaitForVisible('.mini-suggest__item_subtype_ecom-item');
            // ждем истечения фриза кликов в саджесте
            // https://a.yandex-team.ru/arc/trunk/arcadia/frontend/packages/mini-suggest/touch.blocks/mini-suggest/mini-suggest.js?rev=r7787319
            await browser.pause(500);
            await browser.click('.mini-suggest__item_subtype_ecom-item .mini-suggest__item-link');
            await browser.yaWaitForVisible('.EcomScreen_type_product', 'экран с товаром не появился');
            await browser.yaShouldNotBeVisible('.EcomScreen_suggest_visible', 'присутствует модификатор саджеста после перехода');
        });
    });
});

async function mockSuggestResponse(browser, mock) {
    const response = `window[/onSuggestResponse\\d+/.exec(document.currentScript.src)[0]](
        ${typeof mock === 'string' ? mock : JSON.stringify(mock)}
    );`;

    await browser.url('/');
    await browser.then(() => browser.yaStartResourceWatcher(
        '/static/turbo/hermione/mock-external-resources.sw.js',
        [{ url: /onSuggestResponse/g, response }],
    ));
}

function response() {
    return [
        [
            'tpah',
            'карбюратор',
            {
                tpah: [0, 2, 10]
            }
        ],
        [
            'tpah',
            'камера',
            {
                tpah: [0, 2, 6]
            }
        ],
        [
            'fulltext',
            'карбюратор ява',
            {}
        ],
        [
            'fulltext',
            'карбюратор',
            {}
        ],
        [
            'nav',
            'каскад, луч, нева, мб',
            'Запчасти для мотокультиваторов, мотоблоков и минитракторов',
            '/turbo/spideradio.github.io/stub',
            {
                type: 'ecom-category'
            }
        ],
        [
            'nav',
            'навесное оборудование каскад-нева-агрос',
            'Запчасти для мотокультиваторов, мотоблоков и минитракторов',
            '/turbo/spideradio.github.io/stub',
            {
                type: 'ecom-category'
            }
        ],
        [
            'nav',
            'карбюратор 4т 152qmi,157qmj (с подогревом) d24; dingo t150',
            '2 090,00 ₽',
            '/turbo?text=https%3A//spideradio.github.io/rnd/dgwrc&from=suggest',
            {
                type: 'ecom-item',
                img: {
                    url: '/image?width=100&height=100&format=png&patternSize=8',
                    aspect: 'square',
                    contain: true
                }
            }
        ],
        [
            'nav',
            'карбюратор 5т 152qmi,157qmj d24; dingo t150 с подогревом, без активного усилителя',
            '1 090,00 ₽',
            '/turbo/spideradio.github.io/stub',
            {
                type: 'ecom-item',
                img: {
                    url: '/image?width=100&height=100&format=png&patternSize=8',
                    aspect: 'square',
                    contain: true
                }
            }
        ]
    ];
}
