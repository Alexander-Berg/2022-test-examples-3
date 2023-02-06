import { mockAdapterContext } from '@yandex-turbo/applications/beru.ru/mocks/adapterContext';
import BeruReasonsAdapter from '../BeruReasons.adapter';
import { defaultReasonsToBuy, reasonsToProps } from './datastub';
import { IReasonToBuy } from '../BeruReasons.types';

describe('BeruReasonsAdapter', () => {
    describe('метод: transform', () => {
        it('должен правильно подготовить причины для покупки', () => {
            const adapter = new BeruReasonsAdapter(mockAdapterContext());

            expect(adapter.transform({
                block: 'beru-reasons',
                reasons: defaultReasonsToBuy as IReasonToBuy[],
            })).toEqual({
                reasons: reasonsToProps,
            });
        });

        it('должен не приезжать на клиент', () => {
            const adapter = new BeruReasonsAdapter(mockAdapterContext());

            expect(adapter.hasClient()).toBe(false);
        });
    });
});
