import { VENDOR_FILTER_ID, VENDOR_FILTER_TITLE } from '@src/constants/filters';
import { filterProcessStrategy } from '../schema';
import type { TTemplateSimpleFilter } from '../types';

describe('schema', () => {
    describe('filterProcessStrategy', () => {
        it('Переименовывает заголовок фильтра по брендам', () => {
            const templateFilter: TTemplateSimpleFilter = {
                type: 'enum',
                id: VENDOR_FILTER_ID,
                name: 'Производитель',
                values: [
                    { value: 'Apple', found: 1294, id: '153043' },
                    { value: 'Jumper', found: 2, id: '15048524' },
                ],
            };

            const filter = filterProcessStrategy(templateFilter);
            expect(filter.title).toEqual(VENDOR_FILTER_TITLE);
        });
    });
});
