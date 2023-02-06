import {IReviewsGetTrainsReviewsApiResponse} from 'server/api/ReviewsApi/types/IReviewsGetTrainsReviewsApiResponse';

export const NORMALIZED_TRAINS_REVIEWS_MOCK: IReviewsGetTrainsReviewsApiResponse =
    {
        reviews: [
            {
                text: 'Поездка прошла нормально единствено что один туалет не работал и была большая очередь.Сходить в сан узел и умыться мне не удолось',
                rating: '3',
                id: 0,
                trainNumber: '131У',
                updatedAt: '2021-12-21 03:52:36',
            },
            {
                text: 'Я покупал сидячий билет (владивосток - ружино) ВОЗЛЕ СТОЛИКА\r\nно на деле они были не возле столика',
                rating: '4',
                id: 1,
                trainNumber: '002Э',
                updatedAt: '2021-12-18 09:28:02',
            },
            {
                text: 'Всё просто здорово! Современные купе, все аккуратно, чисто. Персонал вежлив и отзывчив. Спасибо!',
                rating: '5',
                id: 2,
                trainNumber: '003С',
                updatedAt: '2021-12-17 11:12:50',
            },
        ],
        directions: [
            ['rostov-don', 'pridacha-voronezh-yuzhniy', [2]],
            ['miass', 'suleya-ru', [0, 1, 2]],
            ['berdyaush-station', 'abdulino', [2, 0]],
        ],
    };
