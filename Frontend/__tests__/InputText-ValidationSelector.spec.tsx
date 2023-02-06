import { selectValidation } from '../ValidationSelector/InputText-ValidationSelector';

describe('InputText-ValidationSelector', () => {
    describe('phone', () => {
        it('isValid() должен учитывать только цифры', () => {
            const isValid = selectValidation('phone')!.isValid('+7 999 1');
            expect(isValid).toBe(false);
        });

        it('isValid() должен возвращать true при достаточной длине телефона', () => {
            const isValid = selectValidation('phone')!.isValid('123456');
            expect(isValid).toBe(true);
        });
    });
});
