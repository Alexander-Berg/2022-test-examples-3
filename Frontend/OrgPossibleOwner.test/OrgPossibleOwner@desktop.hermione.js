'use strict';

const PO = require('./OrgPossibleOwner.page-object').desktop;

specs({
    feature: 'Одна организация',
    type: 'Вы владелец',
}, function() {
    describe('Главный таб в правой колонке', function() {
        const ownerApiUrl = '\/my\/orgs\/[^\/]+\/possibly-owned-by-user';
        const mockResponse = {};
        const mockOptions = {
            recordData: [ownerApiUrl],
            urlDataMap: {
                [ownerApiUrl]: mockResponse,
            },
        };

        beforeEach(async function() {
            const { browser } = this;

            await browser.yaOpenSerp({
                text: '',
                foreverdata: '2425475770',
                data_filter: 'companies',
            }, PO.oneOrg());
            await browser.yaShouldBeVisible(PO.oneOrg.possibleOwner(), 'Нет блока владельца');
        });

        it('Внешний вид', async function() {
            const { browser } = this;

            await browser.yaAssertViewExtended('plain', PO.oneOrg.possibleOwner(), {
                verticalOffset: 30,
                horisontalOffset: 10,
            });
            await browser.setViewportSize({ width: 1050, height: 1000 });
            await browser.assertView('narrow', PO.oneOrg.possibleOwner());
        });

        it('Кнопка подтверждения', async function() {
            const { browser } = this;

            await browser.yaMockXHR(mockOptions);
            await browser.yaCheckLink2({
                selector: PO.oneOrg.possibleOwner.yes(),
                url: {
                    href: 'https://yandex.ru/sprav/verification/byPermalink/99665648394?from=unisearch_candidate',
                },
                baobab: {
                    path: '/$page/$parallel/$result/composite/tabs/about/PossibleOwner/yes',
                },
                message: 'Ошибка в ссылке кнопки подтверждения',
            });
            await browser.yaShouldBeVisible(PO.oneOrg.possibleOwner(), 'Блок владельца скрылся');

            const ajaxRecords = await browser.yaGetXHRRecords(ownerApiUrl);

            assert.deepEqual(ajaxRecords, [{
                url: '/my/orgs/99665648394/possibly-owned-by-user',
                method: 'PUT',
                body: { Value: true },
            }], 'Ошибка в запросе логирования ответа пользователя после подтверждения');
        });

        it('Кнопка опровержения', async function() {
            const { browser } = this;

            await browser.yaMockXHR(mockOptions);
            await browser.click(PO.oneOrg.possibleOwner.no());
            await browser.yaCheckBaobabCounter(() => {}, {
                path: '/$page/$parallel/$result/composite/tabs/about/PossibleOwner/no',
                behaviour: { type: 'dynamic' },
            });
            await browser.yaWaitForHidden(PO.oneOrg.possibleOwner(),
                'Блок владельца не скрылся после клика по кнопке опровержения');

            const ajaxRecords = await browser.yaGetXHRRecords(ownerApiUrl);

            assert.deepEqual(ajaxRecords, [{
                url: '/my/orgs/99665648394/possibly-owned-by-user',
                method: 'PUT',
                body: { Value: false },
            }], 'Ошибка в запросе логирования ответа пользователя после опровержения');
        });

        it('Кнопка закрытия', async function() {
            const { browser } = this;

            await browser.yaMockXHR(mockOptions);
            await browser.click(PO.oneOrg.possibleOwner.close());
            await browser.yaCheckBaobabCounter(() => {}, {
                path: '/$page/$parallel/$result/composite/tabs/about/PossibleOwner/close',
                behaviour: { type: 'dynamic' },
            });
            await browser.yaWaitForHidden(PO.oneOrg.possibleOwner(),
                'Блок владельца не скрылся после клика по кнопке закрытия');

            const ajaxRecords = await browser.yaGetXHRRecords(ownerApiUrl);

            assert.deepEqual(ajaxRecords, [{
                url: '/my/orgs/99665648394/possibly-owned-by-user',
                method: 'PUT',
                body: { Value: false },
            }], 'Ошибка в запросе логирования ответа пользователя после клика на закрыть');
        });
    });
});
