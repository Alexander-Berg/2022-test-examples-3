import { mockAdapterContext } from '@yandex-turbo/applications/beru.ru/mocks/adapterContext';
import * as router from '@yandex-turbo/applications/beru.ru/router';
import BeruSkuFilterAdapter from '../BeruSkuFilter.adapter';
import { IFilterValue } from '../../../applications/beru.ru/interfaces';

describe('BeruSkuFilterAdapter', () => {
    function makeFilterStub(filterValue: Partial<IFilterValue> = {}) {
        return {
            name: 'Цвет',
            type: 'color',
            subType: '',
            kind: 2,
            values: [
                {
                    marketSku: '22222',
                    slug: 'slug',
                    value: '#000',
                    checked: true,
                    image: 'https://path/to/origin',
                    picker: {
                        groupId: '111',
                        entity: 'image',
                        imageName: 'img_1234.jpg',
                        namespace: 'm-pic',
                    },
                    ...filterValue,
                },
            ],
        };
    }

    let instance: BeruSkuFilterAdapter;
    const pushBundleReact = jest.fn();
    const buildUrl = jest.spyOn(router, 'buildUrl').mockImplementation((...params) => {
        return params[0].startsWith('external') ? 'https://path/to/image' : '/turbo?text=https://beru.ru/path/to';
    });

    beforeEach(() => {
        instance = new BeruSkuFilterAdapter(mockAdapterContext({
            assets: {
                pushBundleReact,
            },
        }));

        pushBundleReact.mockClear();
        buildUrl.mockClear();
    });

    describe('метод: transform', () => {
        it('на клиент  должен приезжать модуль BWMNativeScrollOverlay', () => {
            instance.transform({
                block: 'beru-sku-filter',
                filter: makeFilterStub(),
            });

            expect(pushBundleReact).toHaveBeenCalledWith('BWMNativeScrollOverlay');
        });

        it('если в значении фильтра есть поле picker, то должна вызываться ф-ия buildUrl с корректными параметрами', () => {
            instance.transform({
                block: 'beru-sku-filter',
                filter: makeFilterStub(),
            });

            expect(buildUrl).toHaveBeenLastCalledWith('external:photos-storage', {
                imageName: 'img_1234.jpg',
                groupId: '111',
                namespace: 'm-pic',
                size: 2,
            });
        });

        it('если в значении фильтра нет поля picker, то ф-ия buildUrl не должна вызваться', () => {
            instance.transform({
                block: 'beru-sku-filter',
                filter: makeFilterStub({ picker: undefined }),
            });

            expect(buildUrl).not.toHaveBeenNthCalledWith(1, 'external:photos-storage', expect.anything());
        });

        it('если отсутствует в значении фильтра поле "picker.namespace", то при вызове buildUrl в поле "namespace" должно подставляться значение "get-mpic" ', () => {
            instance.transform({
                block: 'beru-sku-filter',
                filter: makeFilterStub({ picker: { namespace: undefined, groupId: '', entity: '', imageName: '' } }),
            });

            expect(buildUrl).toHaveBeenLastCalledWith('external:photos-storage', expect.objectContaining({
                namespace: 'get-mpic',
            }));
        });

        it('buildUrl должен вызываться с верными аргументами, для построения маршрута на карточку товара', () => {
            instance.transform({
                block: 'beru-sku-filter',
                filter: makeFilterStub(),
            });

            expect(buildUrl).toHaveBeenNthCalledWith(1, 'page:product', { skuId: '22222', slug: 'slug' }, { turboLink: true });
        });

        it('должен правильно преобразовывать входные значения фильтра в пропсы компонента', () => {
            expect(instance.transform({
                block: 'beru-sku-filter',
                filter: makeFilterStub(),
            })).toEqual({
                title: 'Цвет:',
                values: [
                    {
                        url: '/turbo?text=https://beru.ru/path/to',
                        value: '#000',
                        checked: true,
                        image: 'https://path/to/image',
                    },
                ],
            });

            expect(instance.transform({
                block: 'beru-sku-filter',
                filter: makeFilterStub({ picker: undefined, checked: undefined }),
            })).toMatchObject({
                values: [
                    {
                        url: '/turbo?text=https://beru.ru/path/to',
                        value: '#000',
                        checked: false,
                        image: 'https://path/to/origin',
                    },
                ],
            });
        });
    });
});
