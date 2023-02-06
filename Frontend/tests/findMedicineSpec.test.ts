import { findMedicineSpec } from '../findMedicineSpec';

const specs = [
    'лекарственный препарат',
    'назначение: лихорадочные состояния, боль в сустава...',
    'действующее вещество: Ибупрофен',
    'возраст: от 12 лет',
    'страна-производитель: Нидерланды',
];

const specsWithoutItem = [
    'лекарственный препарат',
    'назначение: лихорадочные состояния, боль в сустава...',
    'возраст: от 12 лет',
    'страна-производитель: Нидерланды',
];

describe('getText', () => {
    it('должен вернуть корректное значение, если действующее вещ-во есть в массиве', () => {
        expect(findMedicineSpec(specs)).toStrictEqual('Ибупрофен');
    });

    it('должен вернуть undefined, если действующего вещ-ва нет в массиве', () => {
        expect(findMedicineSpec(specsWithoutItem)).toBeUndefined();
    });

    it('корректно обрабатывает разный регистр', () => {
        expect(findMedicineSpec([
            ...specsWithoutItem,
            'Действующее  Вещество : Ибупрофен ',
        ])).toStrictEqual('Ибупрофен');
    });

    it('должен вернуть undefined для пустого массива', () => {
        expect(findMedicineSpec([])).toBeUndefined();
    });
});
