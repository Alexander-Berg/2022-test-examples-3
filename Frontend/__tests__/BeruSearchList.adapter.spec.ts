import { mockAdapterContext } from '@yandex-turbo/applications/beru.ru/mocks/adapterContext';
import * as helpers from '@yandex-turbo/applications/beru.ru/helpers';
import { IKnownThumbnails, IOffer } from '@yandex-turbo/applications/beru.ru/interfaces';
import BeruSearchListAdapter from '../BeruSearchList.adapter';

describe('BeruSearchListAdapter', () => {
    const offers = <IOffer[]> [
        { entity: 'offer', sku: '1' },
        { entity: 'offer', sku: '2' },
    ];
    const thumbnails = <IKnownThumbnails[]> [
        { namespace: 'mpic', thumbnails: [] },
    ];

    describe('метод: transform', () => {
        let getSnippetBaseProps: ReturnType<typeof jest.spyOn>;

        beforeEach(() => {
            getSnippetBaseProps = jest.spyOn(helpers, 'getSnippetBaseProps');
        });

        afterEach(() => {
            getSnippetBaseProps.mockClear();
        });

        it('должен правильно подготавливать пропсы для компоненты', () => {
            const adapter = new BeruSearchListAdapter(mockAdapterContext());
            // @ts-ignore
            getSnippetBaseProps.mockImplementation(offer => {
                return offer;
            });

            expect(adapter.transform({
                block: 'beru-search-list',
                offers,
                thumbnails,
            })).toEqual({
                products: [
                    offers[0], offers[1],
                ],
            });

            // Проверяем только последний вызов
            expect(getSnippetBaseProps).toHaveBeenCalledWith(
                offers[1],
                thumbnails,
                {
                    picture: { size: 100, onlySquare: true },
                    addToCartLink: true,
                    url: true,
                    addToCartMetrika: true,
                    navigateMetrika: true,
                    visibilityMetrika: true,
                }
            );
            expect(getSnippetBaseProps).toHaveBeenCalledTimes(2);
        });

        it('все не валидные оферы должны отфильтровываться', () => {
            const adapter = new BeruSearchListAdapter(mockAdapterContext());
            // @ts-ignore
            const getSnippetBaseProps = jest.spyOn(helpers, 'getSnippetBaseProps').mockImplementation(offer => {
                // Симулируем ситуацию, когда getSnippetBaseProps вернет undefined при работе с сущностью
                // @ts-ignore
                return offer.sku === '1' ? undefined : offer;
            });

            expect(adapter.transform({
                block: 'beru-search-list',
                offers,
                thumbnails,
            })).toEqual({
                products: [
                    offers[1],
                ],
            });

            // Проверяем только последний вызов
            expect(getSnippetBaseProps).toHaveBeenCalledWith(
                offers[1],
                thumbnails,
                {
                    picture: { size: 100, onlySquare: true },
                    addToCartLink: true,
                    url: true,
                    addToCartMetrika: true,
                    navigateMetrika: true,
                    visibilityMetrika: true,
                });
            expect(getSnippetBaseProps).toHaveBeenCalledTimes(2);
        });
    });

    describe('метод: hasClient', () => {
        it('бандл должен приезжать на клиент', () => {
            const adapter = new BeruSearchListAdapter(mockAdapterContext());

            expect(adapter.hasClient()).toEqual(true);
        });
    });
});
