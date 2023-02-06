import getNextResourceIfExists from '../../../../src/helpers/get-next-resource-if-exists';

describe('helper getNextResourceIfExists', () => {
    const resources = [
        { resourceId: '1' },
        { resourceId: '2' },
        { resourceId: '3' },
        { resourceId: '4' },
        { resourceId: '5' }
    ];
    const nullResourceId = { resourceId: null };

    it('Если передан не массив ресурсов или длина массива равна 0 или 1, возвращает объект вида { resourceId: null }', () => {
        const resourceId = '1';

        expect(getNextResourceIfExists(null, resourceId)).toEqual(nullResourceId);
        expect(getNextResourceIfExists([], resourceId)).toEqual(nullResourceId);
        expect(getNextResourceIfExists([resources[0]], resourceId)).toEqual(nullResourceId);
    });

    it('Если ресурс с заданным id не найден, возвращает первый ресурс из массива', () => {
        expect(getNextResourceIfExists(resources, '100')).toEqual(resources[0]);
    });

    it('Если искомый ресурс находится в самом конце массива, возвращает предыдущий ресурс из массива', () => {
        expect(getNextResourceIfExists(resources, '5')).toEqual(resources[3]);
    });

    it('Если ресурс находится не в конце массива, возвращает следующий по порядку ресурс', () => {
        expect(getNextResourceIfExists(resources, '1')).toEqual(resources[1]);
        expect(getNextResourceIfExists(resources, '4')).toEqual(resources[4]);
    });
});
