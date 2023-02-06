import { addFilter } from '../addFilter';

describe('addFilter', () => {
    it('Добавляет значение', () => {
        const glfilter = '17547370:17547372';
        const result = addFilter(glfilter, '7893318', '153043,10556303');
        expect(result).toEqual(['17547370:17547372', '7893318:153043,10556303']);
    });

    it('Добавляет значение в пустой glfilter', () => {
        const result = addFilter(undefined, '7893318', '153043,10556303');
        expect(result).toEqual(['7893318:153043,10556303']);
    });

    it('Убирает значение из glfilter при пустом значении', () => {
        const glfilter = '7893318:153043,10556303';
        const result = addFilter(glfilter, '7893318', '');
        expect(result).toEqual([]);
    });
});
