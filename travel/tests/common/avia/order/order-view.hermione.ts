import moment from 'moment';
import {assert} from 'chai';
import {order} from 'suites/avia';

import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';

const fromPoint = 'Москва';
const toPoint = 'Екатеринбург';
const whenMoment = moment().add(1, 'days');
const when = whenMoment.format('YYYY-MM-DD');

describe(order.name, () => {
    it('Общий вид страницы покупки', async function () {
        const app = new TestAviaApp(this.browser);
        const {searchPage, orderPage} = app;

        await app.goToSearchPage({
            from: {name: 'Москва', id: 'c213'},
            to: {name: 'Екатеринбург', id: 'c54'},
            startDate: when,
            travellers: {
                adults: 1,
                children: 0,
                infants: 0,
            },
            klass: 'economy',
            filters: 'pt=aeroflot',
        });

        await searchPage.waitForSearchComplete();

        const searchVariant = await searchPage.variants.first();

        await searchVariant.moveToOrder();

        await orderPage.waitForLoading();

        const offers = await orderPage.offers.allOffers;

        // layout

        if (orderPage.isTouch) {
            assert(
                await Promise.all([
                    orderPage.bestOffer.logo.isDisplayed(),
                    orderPage.bestOffer.price.isDisplayed(),
                    orderPage.bestOffer.button.isDisplayed(),
                ]),
                'Проблемы с отображением лучшего предложения',
            );
        } else {
            // portal header

            assert(
                await orderPage.header.portalLogo.isDisplayed(),
                'Логотип путешествий не отображен',
            );

            assert(
                await orderPage.header.navigations.areDisplayed([
                    'hotels',
                    'avia',
                    'trains',
                ]),
                'Ссылки на сервисы не отображены',
            );

            assert(
                await orderPage.header.searchInformation.isDisplayed(),
                'Краткая форма поиска не отображена',
            );

            assert(
                await orderPage.header.userInfo.accountLink.isDisplayed(),
                'Иконка личного кабинета не отображена',
            );

            assert(
                await orderPage.header.userInfo.login.isDisplayed(),
                'Кнопка войти не отображена',
            );

            // portal footer

            assert(await orderPage.footer.isDisplayed(), 'Отсутствует футер');

            // avia header

            assert(
                await orderPage.orderHeader.backLink.isDisplayed(),
                'Отсутствует ссылка на поиск',
            );

            assert(
                await Promise.all([
                    orderPage.orderHeader.shareBlock.link.isDisplayed(),
                    orderPage.orderHeader.shareBlock.copy.isDisplayed(),
                ]),
                'Отсутствует сокращатор ссылок',
            );

            assert(
                await Promise.all([
                    orderPage.orderHeader.shareBlock.socialLinks.vk.isDisplayed(),
                    orderPage.orderHeader.shareBlock.socialLinks.twitter.isDisplayed(),
                    orderPage.orderHeader.shareBlock.socialLinks.facebook.isDisplayed(),
                ]),
                'Отсутвуют ссылки социальных сетей',
            );
        }

        // context
        assert.equal(
            await orderPage.offers.title.passengers.getText(),
            '1 пассажир',
            'неверный заголовок с количеством пассажиров',
        );

        assert.equal(
            await orderPage.offers.title.klass.getText(),
            'эконом-класс',
            'неверный заголовок с описанием класса перелёта',
        );

        // offers

        await Promise.all(
            offers.map(async offer => {
                assert(
                    await Promise.all([
                        offer.logo.isDisplayed(),
                        offer.price.isDisplayed(),
                        offer.button.isDisplayed(),
                    ]),
                    'Некорректное отображение предложений партнёров',
                );
            }),
        );

        // flights

        const forwardFlights = await orderPage.forward.flights.items;
        const forwardDatesTitle = await orderPage.forward.title.dates.getText();

        assert.equal(
            await orderPage.forward.title.points.getText(),
            `${fromPoint} — ${toPoint}`,
            'заголовок "туда" не соответствует контексту поиска',
        );

        // не делаем более детального разбора т.к. заголовок с датами
        // может иметь различный вид в зависимости от дат отправления / прибытия
        assert(
            forwardDatesTitle?.startsWith(String(whenMoment.date())),
            `
                неверное отображение даты "туда"
                ожидание: тескт даты начинается с ${whenMoment.date()}
                реальность: ${forwardDatesTitle}
                `,
        );

        await Promise.all(
            forwardFlights.map(async flight =>
                Promise.all([
                    assert(
                        Boolean(await flight.companyTitle.getText()),
                        'Не отобразилось название авиакомпании',
                    ),
                    assert(
                        Boolean(await flight.planeNumber.getText()),
                        'Не отобразился номер рейса',
                    ),
                    assert(
                        Boolean(await flight.timings.departure.getText()),
                        'Не отобразилось время отправления',
                    ),
                    assert(
                        Boolean(await flight.timings.arrival.getText()),
                        'Не отобразилось время прибытия',
                    ),
                    assert(
                        Boolean(await flight.timings.duration.getText()),
                        'Не отобразилось время в пути',
                    ),
                    assert(
                        Boolean(await flight.fromCity.getText()),
                        'Не отобразился пункт отправления',
                    ),
                    assert(
                        Boolean(await flight.toCity.getText()),
                        'Не отобразился пункт прибытия',
                    ),
                    assert(
                        Boolean(await flight.fromAirport.getText()),
                        'Не отобразился аэропорт отправления',
                    ),
                    assert(
                        Boolean(await flight.toAirport.getText()),
                        'Не отобразился аэропорт прибытия',
                    ),
                    assert(
                        Boolean(await flight.fromAirportCode.getText()),
                        'Не отобразился код аэропорта отправления',
                    ),
                    assert(
                        Boolean(await flight.toAirportCode.getText()),
                        'Не отобразился код аэропорта прибытия',
                    ),
                ]),
            ),
        );

        // disclaimers

        assert(
            await Promise.all([
                orderPage.disclaimers.baggage.isDisplayed(),
                orderPage.disclaimers.partners.isDisplayed(),
                orderPage.disclaimers.prices.isDisplayed(),
            ]),
            'Не отобразились дисклеймеры',
        );
    });
});
