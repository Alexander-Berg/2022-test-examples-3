import {getRecentReviews} from 'server/services/ReviewsService/utilities/getRecentReviews';

const FORMATTED_REVIEWS = {
    reviews: {
        0: {
            text: 'Всё просто здорово! Современные купе, все аккуратно, чисто. Персонал вежлив и отзывчив. Спасибо!',
            rating: 5,
            title: 'Поезд 003С',
            updatedAt: '2021-12-17 11:12:50',
        },
        1: {
            text: 'Поездка прошла нормально единствено что один туалет не работал и была большая очередь.Сходить в сан узел и умыться мне не удолось',
            rating: 3,
            title: 'Поезд 131У',
            updatedAt: '2021-12-21 03:52:36',
        },
        2: {
            text: 'Я покупал сидячий билет (владивосток - ружино) ВОЗЛЕ СТОЛИКА\r\nно на деле они были не возле столика',
            rating: 4,
            title: 'Поезд 002Э',
            updatedAt: '2021-12-18 09:28:02',
        },
    },
    directions: {
        'rostov-don--pridacha-voronezh-yuzhniy': [2],
        'miass--suleya-ru': [0, 1, 2],
        'berdyaush-station--abdulino': [2, 0],
    },
};

describe('getRecentReviews', () => {
    it('Вернёт пустой массив по неизвестному направлению', () => {
        expect(getRecentReviews(FORMATTED_REVIEWS, 'moscow--miass')).toEqual(
            [],
        );
    });

    it('Вернёт пустой массив для пустых данных', () => {
        expect(
            getRecentReviews({reviews: {}, directions: {}}, 'miass--suleya-ru'),
        ).toEqual([]);
    });

    it('Вернёт не более указанного кол-ва отзывов отсортированных по дате в обратном порядке', () => {
        expect(
            getRecentReviews(FORMATTED_REVIEWS, 'miass--suleya-ru', 2),
        ).toEqual([FORMATTED_REVIEWS.reviews[1], FORMATTED_REVIEWS.reviews[2]]);
    });
});
