import { isMedicineCategory } from '../isMedicineCategory';

describe('isMedicineCategory', () => {
    it('должен вернуть true, если тип категории - медицина', () => {
        expect(isMedicineCategory({
            name: 'Средства от боли',
            fullName: 'Средства от боли',
            kinds: ['medicine'],
        })).toBe(true);
    });

    it('должен вернуть false, если не указан тип категории', () => {
        expect(isMedicineCategory({
            name: 'Телефоны',
            fullName: 'Телефоны',
            kinds: [],
        })).toBe(false);
    });

    it('должен вернуть false, если нет типов категории', () => {
        expect(isMedicineCategory({
            name: 'Телефоны',
            fullName: 'Телефоны',
        })).toBe(false);
    });

    it('должен вернуть false, если не указана категория', () => {
        expect(isMedicineCategory()).toBe(false);
    });
});
