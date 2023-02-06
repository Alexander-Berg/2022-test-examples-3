import {formatTrainReviews} from 'server/services/ReviewsService/utilities/formatTrainReviews';
import {NORMALIZED_TRAINS_REVIEWS_MOCK} from 'server/services/ReviewsService/utilities/__tests__/__mocks__/normalizedTrainsReviews';

describe('formatTrainReviews', () => {
    it('Вернёт данные в виде справочника с отзывами отсортированными по дате обновления', () => {
        expect(formatTrainReviews(NORMALIZED_TRAINS_REVIEWS_MOCK)).toEqual({
            reviews: {
                0: {
                    text: 'Поездка прошла нормально единствено что один туалет не работал и была большая очередь.Сходить в сан узел и умыться мне не удолось',
                    rating: 3,
                    title: 'Поезд 131У',
                    updatedAt: '2021-12-21 03:52:36',
                },
                1: {
                    text: 'Я покупал сидячий билет (владивосток - ружино) ВОЗЛЕ СТОЛИКА\r\nно на деле они были не возле столика',
                    rating: 4,
                    title: 'Поезд 002Э',
                    updatedAt: '2021-12-18 09:28:02',
                },
                2: {
                    text: 'Всё просто здорово! Современные купе, все аккуратно, чисто. Персонал вежлив и отзывчив. Спасибо!',
                    rating: 5,
                    title: 'Поезд 003С',
                    updatedAt: '2021-12-17 11:12:50',
                },
            },
            directions: {
                'rostov-don--pridacha-voronezh-yuzhniy': [2],
                'miass--suleya-ru': [0, 1, 2],
                'berdyaush-station--abdulino': [2, 0],
            },
        });
    });
});
