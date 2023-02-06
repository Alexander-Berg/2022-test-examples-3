import {assert} from 'chai';
import moment from 'moment';
import {hotel} from 'suites/hotels';
import {random} from 'lodash';

import {TestHotelPage} from 'helpers/project/hotels/pages/HotelPage/TestHotelPage';
import dateFormats from 'helpers/utilities/date/formats';
import extractNumbers from 'helpers/utilities/extractNumbers';
import getHumanMonth from 'helpers/utilities/getHumanMonth';
import {EHotelType, getRandomHotel} from 'helpers/project/hotels/data/hotels';

describe(hotel.name, () => {
    it('Общий вид страницы', async function () {
        const SEARCH_FOR_NIGHTS_COUNT = random(2, 5);
        const checkinDate = moment().add(random(3, 8), 'days');
        const checkoutDate = moment(checkinDate).add(
            SEARCH_FOR_NIGHTS_COUNT,
            'days',
        );
        // Исключаем ГородОтель из-за хостелов
        const {permalink, title, searchTitle, geoText} = getRandomHotel([
            EHotelType.GORODOTEL,
        ]);
        const HOTEL_PARAMS = {
            adults: 1,
            checkinDate: checkinDate.format(dateFormats.ROBOT),
            checkoutDate: checkoutDate.format(dateFormats.ROBOT),
            hotelPermalink: permalink,
        };

        const page = new TestHotelPage(this.browser);

        await page.goToHotel(HOTEL_PARAMS);

        await page.state.waitForLoadingFinished();

        assert.isTrue(
            await page.headerSearch.searchInformation.isVisible(),
            'На уровне хэдера шапка поиска',
        );

        const {direction} =
            await page.headerSearch.searchInformation.getSearchParams();

        assert.equal(direction, searchTitle, `Отель в шапке ${searchTitle}`);

        assert.isTrue(
            (await page.adFox.isVisible()) ||
                (await page.mirCashbackBanner.isVisible()),
            'Под хэдэром баннер рекламы',
        );

        assert.equal(
            await page.backButton.getText(),
            'Отели Москвы',
            'Ниже ссылка на выдачу (с указанием региона)',
        );

        assert.equal(
            await page.hotelName.getText(),
            title,
            `Под ссылкой название отеля ${title}`,
        );
        assert.isTrue(
            await page.hotelAddress.isVisible(),
            'Под названием адрес',
        );
        assert.equal(
            await page.geoFeature.getText(),
            geoText,
            'Под названием отметка "8,8 км до центра"',
        );
        assert.isTrue(
            await page.transportAccessibility.isVisible(),
            'Под названием станция метро',
        );
        assert.isTrue(
            await page.transportAccessibility.distance.isVisible(),
            'Под названием расстоянием до метро.',
        );
        assert.isTrue(
            await page.addToFavorite.isVisible(),
            'Возле названия отеля есть сердечко (добавление в избранное)',
        );
        assert.isTrue(
            await page.rating.isVisible(),
            'Справа от названия рейтинг',
        );

        if (page.isDesktop) {
            assert.isTrue(
                await page.positiveReview.isVisible(),
                'Справа от названия что понравилось гостям',
            );
            assert.isTrue(
                await page.reviewsLink.isVisible(),
                'Справа от названия ссылка на отзывы',
            );
        }

        assert.isTrue(await page.gallery.isVisible(), 'Отображена фото отеля');
        assert.isTrue(
            await page.aboutHotel.isVisible(),
            'Под фото находится блок Про отель',
        );
        assert.isTrue(
            (await page.aboutHotel.mainAmenities.count()) > 0,
            'отображены топ фичи',
        );
        assert.isTrue(
            await page.aboutHotel.toggleAmenities.isVisible(),
            'отображена ссылка "Все услуги"',
        );

        if (page.isDesktop) {
            assert.isTrue(
                await page.geoInfo.isVisible(),
                'Справа расположен блок Отель на карте',
            );
            assert.isTrue(
                await page.geoInfo.address.isVisible(),
                'В нем указан адрес отеля.',
            );
            assert.isTrue(
                await page.geoInfo.map.isVisible(),
                'Под адресом находится карта',
            );
            assert.isTrue(
                await page.geoInfo.mapMarker.isVisible(),
                'Сниппет отеля Novotel есть на карте',
            );
        }

        await page.offersInfo.scrollIntoView();

        assert.isTrue(
            await page.offersInfo.isVisible(),
            'Ниже находится блок номеров',
        );

        assert.isTrue(
            await page.offersInfo.mainOffersTitle.isVisible(),
            'В вверху блока указаны даты и кол-во гостей',
        );

        const numbersFromMainOffersTitle = extractNumbers(
            await page.offersInfo.mainOffersTitle.getText(),
        );

        assert.equal(
            numbersFromMainOffersTitle[0],
            checkinDate.date(),
            'Число месяца заселения в заголовке соответствуют дате на первом шаге кейса',
        );
        assert.include(
            await page.offersInfo.mainOffersTitle.getText(),
            getHumanMonth(checkinDate),
            'Месяц заселения в заголовке соответствуют дате на первом шаге кейса',
        );

        assert.equal(
            numbersFromMainOffersTitle[1],
            checkoutDate.date(),
            'Число месяца выселения в заголовке соответствуют дате на первом шаге кейса',
        );
        assert.include(
            await page.offersInfo.mainOffersTitle.getText(),
            getHumanMonth(checkoutDate),
            'Месяц выселения в заголовке соответствуют дате на первом шаге кейса',
        );

        assert.equal(
            numbersFromMainOffersTitle[2],
            HOTEL_PARAMS.adults,
            'Кол-во гостей в заголовке соответствуют кол-ву на первом шаге кейса',
        );

        assert.isTrue(
            await page.offersInfo.hotelPageSearchForm.isVisible(),
            'Ниже отображается форма поиска',
        );
        assert.match(
            await page.offersInfo.hotelPageSearchForm.period.startTrigger.getText(),
            new RegExp(`${checkinDate.date()} [А-Яа-я]+`),
            'Дата заезда в форме соответствуют введенным данным на первом шаге кейса',
        );
        assert.match(
            await page.offersInfo.hotelPageSearchForm.period.endTrigger.getText(),
            new RegExp(`${checkoutDate.date()} [А-Яа-я]+`),
            'Дата выезда в форме соответствуют введенным данным на первом шаге кейса',
        );
        assert.match(
            await page.offersInfo.hotelPageSearchForm.travellers.trigger.getText(),
            new RegExp(`${HOTEL_PARAMS.adults} [А-Яа-я]+`),
            'Кол-во гостей в форме соответствуют введенным данным на первом шаге кейса',
        );

        if (await page.offersInfo.rooms.roomsWithoutOffers.isVisible()) {
            await page.offersInfo.rooms.roomsWithoutOffers.open();
        }

        const rooms = await page.offersInfo.rooms.rooms;

        assert.isTrue(
            Boolean(await rooms.count()),
            'Ниже расположены предложения',
        );

        await rooms.forEach(async room => {
            assert.isTrue(
                await room.name.isVisible(),
                'Предложение содержит название номера',
            );

            if (page.isDesktop) {
                assert.isTrue(
                    await room.gallery.isVisible(),
                    'Предложение содержит фото или заглушку под фото',
                );
            }

            assert.isTrue(
                await room.bedGroupsAndSize.isVisible(),
                'Предложение содержит указание типов кроватей и плошади',
            );

            assert.isTrue(
                await room.amenities.isVisible(),
                'Предложение содержит топ фичи номера',
            );

            if (page.isDesktop) {
                assert.isTrue(
                    await room.detailedInfoButton.isVisible(),
                    'Предложение содержит ссылку "Подробнее о номере"',
                );
            } else {
                assert.isTrue(
                    await room.amenities.moreButton.isVisible(),
                    'Предложение содержит ссылку "Подробнее о номере"',
                );
            }

            await room.mainOffers.offers.forEach(async offer => {
                assert.isTrue(
                    await offer.hotelOfferLabels.offerMealInfo.isVisible(),
                    'Ниже расположены предложения с указанием информации о питании',
                );
                assert.isTrue(
                    await offer.hotelOfferLabels.hotelsCancellationInfo.trigger.isVisible(),
                    'Ниже расположены предложения с указанием информации о бесплатной отмене',
                );
                assert.isTrue(
                    await offer.price.isVisible(),
                    'Ниже расположены предложения с указанием цены',
                );
                assert.isTrue(
                    await offer.nightsCount.isVisible(),
                    'Ниже расположены предложения с указанием кол-ва ночей',
                );
                assert.isTrue(
                    await offer.plusInfo.isVisible(),
                    'Ниже расположены предложения с указанием кол-ва баллов плюса',
                );
                assert.isTrue(
                    await offer.bookButton.isVisible(),
                    'Ниже расположены предложения с кнопкой Забронировать',
                );
            });
        });

        const similarHotels = await page.similarHotels;

        assert.isTrue(
            Boolean(await similarHotels.count()),
            'Отображен блок похожих отелей',
        );
        await similarHotels.forEach(async similarHotel => {
            assert.isTrue(
                await similarHotel.photo.isVisible(),
                'В блоке есть фото (или заглушка)',
            );

            assert.isTrue(
                await similarHotel.hotelNameWithStars.isVisible(),
                'В блоке есть название отеля',
            );

            assert.isTrue(
                await similarHotel.categoryName.isVisible(),
                'В блоке есть тип (гостиница, хостел и т.д)',
            );

            assert.isTrue(
                await similarHotel.firstOfferPrice.isVisible(),
                'В блоке есть мин цена',
            );
        });

        assert.isTrue(
            await page.hotelFeatures.isVisible(),
            'Ниже расположен блок Гостям понравилось',
        );

        assert.isTrue(
            Boolean(await page.offersInfo.partnerOffers.offers.count()),
            'Под блоком номеров располагается блок цен на номера у партнеров',
        );

        await page.offersInfo.partnerOffers.offers.forEach(
            async partnerOffer => {
                assert.isTrue(
                    await partnerOffer.hotelOperator.name.isVisible(),
                    'В блоке есть название партнера',
                );

                assert.isTrue(
                    await partnerOffer.hotelOperator.icon.isVisible(),
                    'В блоке есть иконка партнера',
                );

                assert.isTrue(
                    await partnerOffer.price.isVisible(),
                    'В блоке есть цена',
                );

                assert.isTrue(
                    await partnerOffer.labels.isVisible(),
                    'В блоке есть информация о питании или условиях отмены',
                );

                assert.isTrue(
                    await partnerOffer.nightsCount.isVisible(),
                    'В блоке указано кол-во ночей',
                );

                assert.isTrue(
                    await partnerOffer.bookButton.isVisible(),
                    'В блоке есть кнопка Перейти',
                );
            },
        );

        assert.isTrue(
            await page.hotelReviews.isVisible(),
            'Еще ниже находится блок отзывов',
        );

        assert.isTrue(
            await page.hotelReviews.title.isVisible(),
            'Есть общее кол-во отзывов',
        );

        assert.isTrue(
            await page.hotelReviews.sortBar.isVisible(),
            'Есть варианты сортировки',
        );

        assert.isTrue(
            await page.hotelReviews.keyPhrases.isVisible(),
            'Есть теги',
        );

        const hotelReviews = await page.hotelReviews.reviewsList;

        assert.isTrue(
            Boolean(await hotelReviews.count()),
            'Затем отображаются отзывы',
        );

        await hotelReviews.forEach(async hotelReview => {
            assert.isTrue(
                await hotelReview.authorName.isVisible(),
                'В каждом отзыве есть имя',
            );

            assert.isTrue(
                await hotelReview.date.isVisible(),
                'В каждом отзыве есть дата отзыва',
            );

            assert.isTrue(
                await hotelReview.stars.isVisible(),
                'В каждом отзыве есть оценка',
            );

            assert.isTrue(
                await hotelReview.text.isVisible(),
                'В каждом отзыве есть текст',
            );

            assert.isTrue(
                (await hotelReview.likeButton.isVisible()) &&
                    (await hotelReview.dislikeButton.isVisible()),
                'В каждом отзыве есть икноки лайка и дизлайка',
            );
        });

        assert.isTrue(
            await page.hotelReviews.moreReviewsButton.isVisible(),
            'Под последним отзывом есть ссылка Еще отзывы',
        );

        const breadcrumbs = await page.breadcrumbs.items;

        assert.isTrue(
            Boolean(breadcrumbs.length),
            'Ниже отображаются хлебные крошки',
        );
        assert.isNull(
            await breadcrumbs[breadcrumbs.length - 1].getAttribute('href'),
            'Название отеля некликабельно',
        );
    });
});
