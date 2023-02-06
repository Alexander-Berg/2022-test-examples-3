'use strict';

const PO = require('./OrgPossibleOwner.page-object').touchPhone;

specs({
    feature: 'Одна организация',
    type: 'Вы владелец',
}, function() {
    describe('На Серпе', function() {
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
                foreverdata: '2672958589',
                data_filter: 'companies',
            }, PO.oneOrg());
            await browser.yaShouldBeVisible(PO.oneOrg.possibleOwner(), 'Нет блока владельца');
        });

        hermione.also.in('iphone-dark');
        it('Внешний вид', async function() {
            const { browser } = this;

            await browser.yaAssertViewExtended('plain', PO.oneOrg.possibleOwner(), {
                verticalOffset: 25,
                horisontalOffset: 10,
            });
        });

        hermione.only.notIn(['iphone'], 'orientation is not supported');
        it('Ландшафтная ориентация', async function() {
            const { browser } = this;

            await browser.setOrientation('landscape');
            await browser.assertView('landscape', PO.oneOrg.possibleOwner());
        });

        it('Кнопка подтверждения', async function() {
            const { browser } = this;

            await browser.yaMockXHR(mockOptions);
            await browser.yaCheckLink2({
                selector: PO.oneOrg.possibleOwner.yes(),
                url: {
                    href: 'https://yandex.ru/sprav/verification/byPermalink/99665648394?from=unisearch_candidate&utm_source=unisearch_touch',
                },
                baobab: {
                    path: '/$page/$main/$result/composite/PossibleOwner/yes',
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
                path: '/$page/$main/$result/composite/PossibleOwner/no',
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
                path: '/$page/$main/$result/composite/PossibleOwner/close',
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
