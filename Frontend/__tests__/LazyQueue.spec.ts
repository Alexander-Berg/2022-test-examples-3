import { ELazyTypes, LazyQueue } from '../LazyQueue';

let lazyQueue: LazyQueue;
let callOrder: Array<number | string>;

beforeEach(() => {
    jest.useFakeTimers();
    lazyQueue = new LazyQueue();
    lazyQueue.releaseAll();

    callOrder = [];
});

afterEach(() => jest.useRealTimers());

describe('Лимит одновременных загрузок', () => {
    function pushToQueue(type: ELazyTypes, count: number) {
        for (let i = 1; i <= count; i++) {
            lazyQueue.push(type, () => callOrder.push(i));
        }
    }

    const tests = [
        {
            name: 'Загружает не больше 2х реклам одновременно',
            type: ELazyTypes.ADVERT,
            expected: [1, 2],
        },
        {
            name: 'Загружает не больше 2х эмбедов одновременно',
            type: ELazyTypes.EMBED,
            expected: [1, 2],
        },
        {
            name: 'Загружает не больше 6ти картинок одновременно',
            type: ELazyTypes.IMAGE,
            expected: [1, 2, 3, 4, 5, 6],
        },
    ];

    for (const test of tests) {
        it(test.name, () => {
            pushToQueue(test.type, test.expected.length + 2);

            jest.runAllTimers();
            expect(callOrder).toEqual(test.expected);
        });
    }
});

describe('Освобождение ресусов', () => {
    it('Разные ресурсы загружаются вместе', () => {
        lazyQueue.push(ELazyTypes.EMBED, () => callOrder.push('1e'));
        jest.runAllTimers();
        lazyQueue.push(ELazyTypes.IMAGE, () => callOrder.push('1i'));
        jest.runAllTimers();
        lazyQueue.push(ELazyTypes.EMBED, () => callOrder.push('2e'));
        jest.runAllTimers();
        lazyQueue.push(ELazyTypes.IMAGE, () => callOrder.push('2i'));
        jest.runAllTimers();
        lazyQueue.push(ELazyTypes.EMBED, () => callOrder.push('3e'));
        jest.runAllTimers();
        lazyQueue.push(ELazyTypes.IMAGE, () => callOrder.push('3i'));
        jest.runAllTimers();

        expect(callOrder).toEqual(['1e', '1i', '2e', '2i', '3i']);
    });

    it('Освобождает ресурс по uniqId', () => {
        lazyQueue.push(ELazyTypes.EMBED, () => callOrder.push('1e'));
        jest.runAllTimers();
        const embedId1 = lazyQueue.push(ELazyTypes.EMBED, () => callOrder.push('2e'));
        const embedId2 = lazyQueue.push(ELazyTypes.EMBED, () => callOrder.push('3e'));
        lazyQueue.push(ELazyTypes.EMBED, () => callOrder.push('4e'));
        jest.runAllTimers();

        const advertId = lazyQueue.push(ELazyTypes.ADVERT, () => callOrder.push('1a'));
        lazyQueue.push(ELazyTypes.ADVERT, () => callOrder.push('2a'));
        jest.runAllTimers();
        lazyQueue.push(ELazyTypes.ADVERT, () => callOrder.push('3a'));
        jest.runAllTimers();

        expect(callOrder).toEqual(['1e', '2e', '1a', '2a']);

        lazyQueue.release(ELazyTypes.EMBED, embedId1);

        expect(callOrder).toEqual(['1e', '2e', '1a', '2a', '3e']);

        lazyQueue.release(ELazyTypes.ADVERT, advertId);

        expect(callOrder).toEqual(['1e', '2e', '1a', '2a', '3e', '3a']);

        lazyQueue.release(ELazyTypes.EMBED, embedId2);

        expect(callOrder).toEqual(['1e', '2e', '1a', '2a', '3e', '3a', '4e']);
    });

    it('Не освобождает ресурс, если type и uniqId не совпадают', () => {
        lazyQueue.push(ELazyTypes.EMBED, () => callOrder.push('1e'));
        jest.runAllTimers();
        lazyQueue.push(ELazyTypes.EMBED, () => callOrder.push('2e'));
        jest.runAllTimers();
        lazyQueue.push(ELazyTypes.EMBED, () => callOrder.push('3e'));
        jest.runAllTimers();
        const advertId = lazyQueue.push(ELazyTypes.ADVERT, () => callOrder.push('1a'));
        lazyQueue.push(ELazyTypes.ADVERT, () => callOrder.push('2a'));
        jest.runAllTimers();

        lazyQueue.release(ELazyTypes.EMBED, advertId);

        expect(callOrder).toEqual(['1e', '2e', '1a', '2a']);
    });

    it('Не освобождает 1 ресурс 2 раза', () => {
        const uniqId = lazyQueue.push(ELazyTypes.EMBED, () => callOrder.push(1));
        lazyQueue.push(ELazyTypes.EMBED, () => callOrder.push(2));
        jest.runAllTimers();
        lazyQueue.push(ELazyTypes.EMBED, () => callOrder.push(3));
        jest.runAllTimers();
        lazyQueue.push(ELazyTypes.EMBED, () => callOrder.push(4));
        jest.runAllTimers();

        lazyQueue.release(ELazyTypes.EMBED, uniqId);

        expect(callOrder).toEqual([1, 2, 3]);

        lazyQueue.release(ELazyTypes.EMBED, uniqId);

        expect(callOrder).toEqual([1, 2, 3]);
    });
});
