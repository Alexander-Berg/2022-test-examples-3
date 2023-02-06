import { act } from '@testing-library/react';
import { getBotCounterResponse, getCounterResponse } from '~/test/jest/mocks/data/resources-counter';
import { render, ResourcesCounterRootFragment } from './resources-counter.po';

describe('Карточки поставщиков ресурсов', () => {
    it('1. Отображение списка поставщиков ресурсов', done => {
        let resourcesCounter: ResourcesCounterRootFragment | null = null;
        act(() => {
            resourcesCounter = render({}, Promise.resolve(getCounterResponse()));
        });

        setTimeout(() => {
            /* eslint-disable @typescript-eslint/no-non-null-assertion */
            expect(resourcesCounter!.cards.length).toBe(2);

            expect(resourcesCounter!.cards[0].url).toBe('?view=consuming&layout=table&supplier=391');
            expect(resourcesCounter!.cards[0].title).toBe('Кондуктор');
            expect(resourcesCounter!.cards[0].features.length).toBe(2);
            expect(resourcesCounter!.cards[0].features[0].url).toBe('?view=consuming&layout=table&supplier=391&type=19');
            expect(resourcesCounter!.cards[0].features[0].name).toBe('project');
            expect(resourcesCounter!.cards[0].features[0].value).toBe('—');
            expect(resourcesCounter!.cards[0].features[1].url).toBe('?view=consuming&layout=table&supplier=391&type=20');
            expect(resourcesCounter!.cards[0].features[1].name).toBe('group');
            expect(resourcesCounter!.cards[0].features[1].value).toBe('2');

            expect(resourcesCounter!.cards[1].url).toBe('?view=consuming&layout=table&supplier=392');
            expect(resourcesCounter!.cards[1].title).toBe('Кондуктор-2');
            expect(resourcesCounter!.cards[1].features.length).toBe(1);
            expect(resourcesCounter!.cards[1].features[0].url).toBe('?view=consuming&layout=table&supplier=392&type=21');
            expect(resourcesCounter!.cards[1].features[0].name).toBe('host');
            expect(resourcesCounter!.cards[1].features[0].value).toBe('16,384');
            /* eslint-enable @typescript-eslint/no-non-null-assertion */

            done();
        }, 1);
    });

    it('2. Длинный список типов агрегируется', done => {
        let resourcesCounter: ResourcesCounterRootFragment | null = null;
        act(() => {
            resourcesCounter = render({}, Promise.resolve(getBotCounterResponse()));
        });

        setTimeout(() => {
            /* eslint-disable @typescript-eslint/no-non-null-assertion */
            expect(resourcesCounter!.cards.length).toBe(1);

            expect(resourcesCounter!.cards[0].url).toBe('?view=consuming&layout=table&supplier=385');
            expect(resourcesCounter!.cards[0].title).toBe('BOT');
            expect(resourcesCounter!.cards[0].features.length).toBe(1);
            expect(resourcesCounter!.cards[0].features[0].url).toBe('?view=consuming&layout=table&supplier=385');
            expect(resourcesCounter!.cards[0].features[0].name).toBe('i18n:all-types');
            expect(resourcesCounter!.cards[0].features[0].value).toBe('23');
            /* eslint-enable @typescript-eslint/no-non-null-assertion */

            done();
        }, 1);
    });

    it('3. Во время ожидания загрузки отображается спиннер', done => {
        jest.spyOn(window, 'fetch').mockImplementation(async url => {
            if (typeof url === 'string' && new URL(url).pathname === '/back-proxy/api/frontend/resources/counter/') {
                /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
                return new Promise(() => { }) as any;
            }
            return { ok: true, json: async() => ({}) };
        });

        let resourcesCounter: ResourcesCounterRootFragment | null = null;
        act(() => {
            resourcesCounter = render({}, new Promise(() => { }));
        });

        setTimeout(() => {
            /* eslint-disable @typescript-eslint/no-non-null-assertion */
            expect(resourcesCounter!.cards.length).toBe(0);

            expect(resourcesCounter!.spinner?.container).toBeInTheDocument();
            /* eslint-enable @typescript-eslint/no-non-null-assertion */

            done();
        }, 1);
    });
});
