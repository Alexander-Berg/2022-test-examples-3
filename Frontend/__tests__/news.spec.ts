import {
    getTimezone,
    getReactionById,
    getImgSrc,
} from '../functions/common';

import {
    ITzInfo,
    IReactionList,
} from '../backend-types';

describe('getTimezone', () => {
    const timezoneTuned = {
        tuned: {
            offset: 10800,
        },
        real: {
            offset: 14400,
        },
        default: {
            offset: 18000,
        },
    } as ITzInfo;

    const timezoneReal = {
        tuned: {},
        real: {
            offset: 14400,
        },
        default: {
            offset: 18000,
        },
    } as ITzInfo;

    const timezoneDefault = {
        tuned: {},
        real: {},
        default: {
            offset: 18000,
        },
    } as ITzInfo;

    const noTimezone = {
        tuned: {},
        real: {},
        default: {},
    } as ITzInfo;

    it('Присутствует tuned offset', () => {
        const timezoneOffset = 3;
        const tunedTimezone = getTimezone(timezoneTuned);
        expect(tunedTimezone).toBe(timezoneOffset);
    });

    it('Присутствует real offset, но отсутствует tuned offset, ', () => {
        const timezoneOffset = 4;
        const realTimezone = getTimezone(timezoneReal);
        expect(realTimezone).toBe(timezoneOffset);
    });

    it('Отсутствует tuned и real offset', () => {
        const timezoneOffset = 5;
        const defaultTimezone = getTimezone(timezoneDefault);
        expect(defaultTimezone).toBe(timezoneOffset);
    });

    it('Отсутствует offset', () => {
        const defaultTimezone = getTimezone(noTimezone);
        expect(defaultTimezone).toBe(0);
    });
});

describe('getReactionById', () => {
    const reactions = {
        aggregate: {
            '62431261': {
                LikeCount: 0,
            },
            '62664966': {
                LikeCount: 15,
            },
        },
        userReaction: {
            '62664966': {
                Like: true,
            },
        },
    } as IReactionList;

    const expectedReactions = {
        isLiked: false,
        likeCount: 0,
    };

    const expectedReactionsWithUserLike = {
        isLiked: true,
        likeCount: 15,
    };

    it('Должен соответствовать ожидаемой реакции', () => {
        expect(getReactionById(reactions, '62431261')).toEqual(expectedReactions);
        expect(getReactionById(reactions, '62664966')).toEqual(expectedReactionsWithUserLike);
    });
});

describe('getImgSrc', () => {
    const picture = {
        groupId: 55952,
        url: 'https://s-cdn.sportbox.ru/images/690_388.jpg',
        width: 770,
        height: 434,
    };

    it('Без указанных размеров картинки должен возвращать url картинки', () => {
        const expectedUrl = {
            imgSrc: 'https://avatars.mds.yandex.net/get-ynews/55952/a42658b41f6555d4913fb4a7e6a8e470/496x248',
            imgSrcSet: undefined,
        };
        expect(getImgSrc({ picture })).toEqual(expectedUrl);
    });

    it('с указанными размерами картинки должен возвращать url картинки с заданным размером', () => {
        const expectedUrl = {
            imgSrc: 'https://avatars.mds.yandex.net/get-ynews/55952/a42658b41f6555d4913fb4a7e6a8e470/770x434',
            imgSrcSet: undefined,
        };
        const size = {
            width: 770,
            height: 434,
        };
        expect(getImgSrc({ picture, size })).toEqual(expectedUrl);
    });

    it('с указанным размером вдвое большим размера картинки должен возвращать imgSrcSet', () => {
        const expectedUrl = {
            imgSrc: 'https://avatars.mds.yandex.net/get-ynews/55952/a42658b41f6555d4913fb4a7e6a8e470/385x217',
            imgSrcSet: 'https://avatars.mds.yandex.net/get-ynews/55952/a42658b41f6555d4913fb4a7e6a8e470/770x434 2x',
        };
        const size = {
            width: 385,
            height: 217,
        };
        expect(getImgSrc({ picture, size })).toEqual(expectedUrl);
    });
});
