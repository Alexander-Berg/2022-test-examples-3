import { applyPatch, getMessages } from '../utils';

describe('ConversationController:utils', () => {
    describe('getMessages', () => {
        const cache = new Map([
            [1, { timestamp: 1, prevTimestamp: 0, nextTimestamp: 2 }],
            [2, { timestamp: 2, prevTimestamp: 1 }],
            [4, { timestamp: 4, prevTimestamp: 3, nextTimestamp: 5 }],
            [5, { timestamp: 5, prevTimestamp: 4, nextTimestamp: 6 }],
            [6, { timestamp: 6, prevTimestamp: 5 }],
        ]);

        const cacheWithoutHoles = new Map([
            [1, { timestamp: 1, prevTimestamp: 0, nextTimestamp: 2 }],
            [2, { timestamp: 2, prevTimestamp: 1, nextTimestamp: 3 }],
            [3, { timestamp: 3, prevTimestamp: 2, nextTimestamp: 4 }],
            [4, { timestamp: 4, prevTimestamp: 3, nextTimestamp: 5 }],
            [5, { timestamp: 5, prevTimestamp: 4, nextTimestamp: 6 }],
            [6, { timestamp: 6, prevTimestamp: 5 }],
        ]);

        it('Запрос по maxTimestamp + limit', () => {
            expect(getMessages({
                cache,
                lastTimestamp: 6,
                maxTimestamp: 6,
                limit: 3,
            })).toMatchObject({
                timestamps: [4, 5, 6],
                beginTs: 4,
                endTs: 6,
                isLast: true,
                isFirst: false,
                size: 3,
                head: 3,
                tail: 0,
                minTimestampGot: false,
                fullfield: true,
            });
        });

        it('Запрос по maxTimestamp + limit в случае неконсистентной истории', () => {
            expect(getMessages({
                cache,
                lastTimestamp: 6,
                maxTimestamp: 6,
                limit: 4,
            })).toMatchObject({
                timestamps: [4, 5, 6],
                beginTs: 4,
                endTs: 6,
                isLast: true,
                isFirst: false,
                size: 3,
                head: 3,
                tail: 0,
                minTimestampGot: false,
                fullfield: false,
            });
        });

        it('Запрос по maxTimestamp + limit в случае нехватки сообщений', () => {
            expect(getMessages({
                cache,
                lastTimestamp: 6,
                maxTimestamp: 2,
                limit: 4,
            })).toMatchObject({
                timestamps: [1, 2],
                beginTs: 1,
                endTs: 2,
                isLast: false,
                isFirst: true,
                size: 2,
                head: 2,
                tail: 0,
                minTimestampGot: false,
                fullfield: true,
            });
        });

        it('Запрос по maxTimestamp + limit, если сообщение последнее', () => {
            expect(getMessages({
                cache,
                lastTimestamp: 6,
                maxTimestamp: 1,
                limit: 1,
            })).toMatchObject({
                timestamps: [1],
                beginTs: 1,
                endTs: 1,
                isLast: false,
                isFirst: true,
                size: 1,
                head: 1,
                tail: 0,
                minTimestampGot: false,
                fullfield: true,
            });
        });

        it('Запрос по maxTimestamp + limit + offset', () => {
            expect(getMessages({
                cache,
                lastTimestamp: 6,
                maxTimestamp: 5,
                limit: 3,
                offset: 1,
            })).toMatchObject({
                timestamps: [4, 5, 6],
                beginTs: 4,
                endTs: 6,
                isLast: true,
                isFirst: false,
                size: 3,
                head: 2,
                tail: 1,
                minTimestampGot: false,
                fullfield: true,
            });
        });

        it('Полная история', () => {
            expect(getMessages({
                cache: cacheWithoutHoles,
                lastTimestamp: 6,
                maxTimestamp: 6,
                limit: 6,
            })).toMatchObject({
                timestamps: [1, 2, 3, 4, 5, 6],
                beginTs: 1,
                endTs: 6,
                isLast: true,
                isFirst: true,
                size: 6,
                head: 6,
                tail: 0,
                minTimestampGot: false,
                fullfield: true,
            });
        });

        it('Фильтрация, нужно кол-во не набрано, но в чате больше нет сообщений', () => {
            expect(getMessages({
                cache: cacheWithoutHoles,
                lastTimestamp: 6,
                maxTimestamp: 6,
                limit: 6,
                filter: ({ timestamp }) => {
                    return timestamp > 2 && timestamp < 5;
                },
            })).toMatchObject({
                timestamps: [3, 4],
                beginTs: 1,
                endTs: 6,
                isLast: true,
                isFirst: true,
                size: 6,
                head: 2,
                tail: 0,
                minTimestampGot: false,
                fullfield: true,
            });
        });

        it('Фильтрация, нужно кол-во набрано', () => {
            expect(getMessages({
                cache: cacheWithoutHoles,
                lastTimestamp: 6,
                maxTimestamp: 6,
                limit: 2,
                filter: ({ timestamp }) => {
                    return timestamp > 2 && timestamp < 5;
                },
            })).toMatchObject({
                timestamps: [3, 4],
                beginTs: 3,
                endTs: 6,
                isLast: true,
                isFirst: false,
                size: 4,
                head: 2,
                tail: 0,
                minTimestampGot: false,
                fullfield: true,
            });
        });

        it('Фильтрация с offset', () => {
            expect(getMessages({
                cache: cacheWithoutHoles,
                lastTimestamp: 6,
                maxTimestamp: 3,
                offset: 3,
                limit: 6,
                filter: ({ timestamp }) => {
                    return timestamp > 2 && timestamp < 5;
                },
            })).toMatchObject({
                timestamps: [3, 4],
                beginTs: 1,
                endTs: 6,
                isLast: true,
                isFirst: true,
                size: 6,
                head: 1,
                tail: 1,
                minTimestampGot: false,
                fullfield: true,
            });
        });

        it('Фильтрация с offset не доходя до конца', () => {
            expect(getMessages({
                cache: cacheWithoutHoles,
                lastTimestamp: 6,
                maxTimestamp: 2,
                offset: 2,
                limit: 6,
                filter: ({ timestamp }) => {
                    return timestamp > 2 && timestamp < 5;
                },
            })).toMatchObject({
                timestamps: [3, 4],
                beginTs: 1,
                endTs: 4,
                isLast: false,
                isFirst: true,
                size: 4,
                head: 0,
                tail: 2,
                minTimestampGot: false,
                fullfield: true,
            });
        });

        it('Запрос с maxTimestamp, minTimestamp, limit', () => {
            expect(getMessages({
                cache: cacheWithoutHoles,
                lastTimestamp: 6,
                maxTimestamp: 5,
                minTimestamp: 2,
                limit: 6,
            })).toMatchObject({
                timestamps: [3, 4, 5],
                beginTs: 3,
                endTs: 5,
                isLast: false,
                isFirst: false,
                size: 3,
                head: 3,
                tail: 0,
                minTimestampGot: true,
                fullfield: true,
            });
        });

        it('Запрос с maxTimestamp, minTimestamp, limit, exclude', () => {
            expect(getMessages({
                cache: cacheWithoutHoles,
                lastTimestamp: 6,
                maxTimestamp: 6,
                minTimestamp: 2,
                exclude: true,
                limit: 6,
            })).toMatchObject({
                timestamps: [3, 4, 5],
                beginTs: 3,
                endTs: 5,
                isLast: false,
                isFirst: false,
                size: 3,
                head: 3,
                tail: 0,
                minTimestampGot: true,
                fullfield: true,
            });
        });

        it('Запрос с maxTimestamp, minTimestamp, limit, offset', () => {
            expect(getMessages({
                cache: cacheWithoutHoles,
                lastTimestamp: 6,
                maxTimestamp: 3,
                minTimestamp: 1,
                offset: 4,
                limit: 10,
            })).toMatchObject({
                timestamps: [2, 3, 4, 5, 6],
                beginTs: 2,
                endTs: 6,
                isLast: true,
                isFirst: false,
                size: 5,
                head: 2,
                tail: 3,
                minTimestampGot: true,
                fullfield: true,
            });
        });

        it('Запрос с maxTimestamp === последнему сообщению, limit, offset', () => {
            expect(getMessages({
                cache: cacheWithoutHoles,
                lastTimestamp: 6,
                maxTimestamp: 6,
                offset: 4,
                limit: 5,
            })).toMatchObject({
                timestamps: [2, 3, 4, 5, 6],
                beginTs: 2,
                endTs: 6,
                isLast: true,
                isFirst: false,
                size: 5,
                head: 5,
                tail: 0,
                minTimestampGot: false,
                fullfield: true,
            });
        });

        it('Запрос с maxTimestamp === первому сообщению, limit, offset', () => {
            expect(getMessages({
                cache: cacheWithoutHoles,
                lastTimestamp: 6,
                maxTimestamp: 1,
                offset: 4,
                limit: 5,
            })).toMatchObject({
                timestamps: [1, 2, 3, 4, 5],
                beginTs: 1,
                endTs: 5,
                isLast: false,
                isFirst: true,
                size: 5,
                head: 1,
                tail: 4,
                minTimestampGot: false,
                fullfield: true,
            });
        });

        it('Запрос вместе с удаленной историей чата', () => {
            expect(getMessages({
                cache,
                lastTimestamp: 6,
                maxTimestamp: 6,
                historyStartTs: 4,
                limit: 5,
            })).toMatchObject({
                timestamps: [5, 6],
                beginTs: 5,
                endTs: 6,
                isLast: true,
                isFirst: true,
                size: 2,
                head: 2,
                tail: 0,
                minTimestampGot: false,
                fullfield: true,
            });
        });

        it('Запрос одного значения с ограничением historyStartTs', () => {
            expect(getMessages({
                cache,
                lastTimestamp: 6,
                maxTimestamp: 4,
                historyStartTs: 3,
                limit: 1,
            })).toMatchObject({
                timestamps: [4],
                beginTs: 4,
                endTs: 4,
                isLast: false,
                isFirst: true,
                size: 1,
                head: 1,
                tail: 0,
                minTimestampGot: false,
                fullfield: true,
            });
        });

        it('Запрос отсутствующего значения с ограничением historyStartTs', () => {
            expect(getMessages({
                cache,
                lastTimestamp: 6,
                maxTimestamp: 3,
                historyStartTs: 2,
                limit: 1,
            })).toMatchObject({
                timestamps: [],
                beginTs: 0,
                endTs: 0,
                isLast: false,
                isFirst: false,
                size: 0,
                head: 0,
                tail: 0,
                minTimestampGot: false,
                fullfield: false,
            });
        });

        it('Запрос истории до historyStartTs', () => {
            expect(getMessages({
                cache,
                lastTimestamp: 6,
                maxTimestamp: 2,
                historyStartTs: 5,
                limit: 1,
            })).toMatchObject({
                timestamps: [],
                beginTs: 0,
                endTs: 0,
                isLast: false,
                isFirst: false,
                size: 0,
                head: 0,
                tail: 0,
                minTimestampGot: false,
                fullfield: false,
            });
        });
    });

    describe('applyPatch', () => {
        it('Патч должен добавить между элементами', () => {
            expect(applyPatch(
                [1, 2, 3],
                [0, 4, 5, 6],
            )).toEqual([0, 1, 2, 3, 4, 5, 6]);
        });

        it('Патч должен добавиться в начало', () => {
            expect(applyPatch(
                [1, 2, 3],
                [4, 5, 6],
            )).toEqual([1, 2, 3, 4, 5, 6]);
        });

        it('Патч должен добавиться в конец', () => {
            expect(applyPatch(
                [7, 8, 9],
                [4, 5, 6],
            )).toEqual([4, 5, 6, 7, 8, 9]);
        });

        it('Патч должен добавиться в конец заменив последний элемент', () => {
            expect(applyPatch(
                [6, 7, 8, 9],
                [4, 5, 6],
            )).toEqual([4, 5, 6, 7, 8, 9]);
        });

        it('Одноэлементный патч', () => {
            expect(applyPatch(
                [6],
                [4, 5, 6, 7],
            )).toEqual([4, 5, 6, 7]);

            expect(applyPatch(
                [4],
                [4, 5, 6, 7],
            )).toEqual([4, 5, 6, 7]);

            expect(applyPatch(
                [5],
                [4, 5, 6, 7],
            )).toEqual([4, 5, 6, 7]);

            expect(applyPatch(
                [7],
                [4, 5, 6, 8],
            )).toEqual([4, 5, 6, 7, 8]);

            expect(applyPatch(
                [5],
                [4, 6, 8],
            )).toEqual([4, 5, 6, 8]);

            expect(applyPatch(
                [9],
                [4, 5, 6, 8],
            )).toEqual([4, 5, 6, 8, 9]);

            expect(applyPatch(
                [3],
                [4, 5, 6, 8],
            )).toEqual([3, 4, 5, 6, 8]);
        });

        it('Патч должен правильно обрабатывать перекрытия', () => {
            expect(applyPatch(
                [1, 2, 3, 4],
                [0, 4, 5, 6],
            )).toEqual([0, 1, 2, 3, 4, 5, 6]);

            expect(applyPatch(
                [0, 1, 2, 3, 4],
                [0, 4, 5, 6],
            )).toEqual([0, 1, 2, 3, 4, 5, 6]);

            expect(applyPatch(
                [0, 1, 7],
                [0, 4, 5, 6],
            )).toEqual([0, 1, 7]);

            expect(applyPatch(
                [0, 1, 6],
                [0, 4, 5, 7],
            )).toEqual([0, 1, 6, 7]);

            expect(applyPatch(
                [0, 1, 6],
                [0, 4, 5, 6, 7],
            )).toEqual([0, 1, 6, 7]);

            expect(applyPatch(
                [7, 8, 9],
                [0, 4, 5, 6, 7],
            )).toEqual([0, 4, 5, 6, 7, 8, 9]);
        });
    });
});
