import {groupMetaclassesByParent, filterMetaclassesByQuery} from '../helpers';
import {metaclasses, expectedGrouppedMetaclasses, expectedFilteredMetaclasses} from './mock';

describe('groupMetaclassesByParent', () => {
    it('возвращает сгруппированную по полю parent хеш-таблицу метаклассов', () => {
        const grouppedMetaclasses = groupMetaclassesByParent(metaclasses);

        expect(grouppedMetaclasses).toEqual(expectedGrouppedMetaclasses);
    });
});

describe('filterMetaclassesByQuery', () => {
    it('возврашает метаклассы, название которых содержит запрос и всех родителей этих метаклассов', () => {
        const filiteredMetaclassesWithParents = filterMetaclassesByQuery(metaclasses, 'Возврат');

        expect(filiteredMetaclassesWithParents).toEqual(expectedFilteredMetaclasses);
    });

    it('возврашает метаклассы, fqn которых содержит запрос и всех родителей этих метаклассов', () => {
        const filiteredMetaclassesWithParents = filterMetaclassesByQuery(metaclasses, 'refund');

        expect(filiteredMetaclassesWithParents).toEqual(expectedFilteredMetaclasses);
    });
});
