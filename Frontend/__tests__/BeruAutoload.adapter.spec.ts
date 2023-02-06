import { mockAdapterContext } from '@yandex-turbo/applications/beru.ru/mocks/adapterContext';
import * as adapterAndRRHelpers from '@yandex-turbo/applications/beru.ru/helpers/adapter-rrCtx';
import BeruAutoloadAdapter from '../BeruAutoload.adapter';

describe('BeruAutoloadAdapter', () => {
    let instance: BeruAutoloadAdapter;
    const context = mockAdapterContext();

    beforeEach(() => {
        instance = new BeruAutoloadAdapter(context);
    });

    describe('метод: transform', () => {
        it('должен правильно подготавливать пропсы для компоненты', () => {
            const expectedUrl = `http://localhost/turbo?text=${encodeURIComponent('https://project.ru/path/to')}`;
            const makeTurboUrl = jest.spyOn(adapterAndRRHelpers, 'makeTurboUrl').mockReturnValue(expectedUrl);

            const result = instance.transform({
                block: 'beru-autoload',
                url: 'https://project.ru/path/to',
                className: 'test',
                moveTo: '.elem',
                stub: 'default',
                button: {},
            });

            expect(result).toEqual({
                url: expectedUrl,
                className: 'test',
                moveTo: '.elem',
                stub: 'default',
                button: {},
            });
            expect(makeTurboUrl).toHaveBeenCalledTimes(1);
        });
    });
});
