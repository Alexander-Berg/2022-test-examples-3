import _isEmpty from 'lodash/isEmpty';
import _cloneDeep from 'lodash/cloneDeep';

import {
    groupFormFalues,
    groupFormValidationInfo,
} from './__mocks__/groupDependsValues';
import validateValues from '../validateValues';

describe('Валидация данных формы без групп и массивов', () => {
    let formValues = groupFormFalues;
    let validationInfo = groupFormValidationInfo;

    beforeEach(() => {
        formValues = _cloneDeep(groupFormFalues);
        validationInfo = _cloneDeep(groupFormValidationInfo);
    });

    it('Зависимая валидация с указанием на другую группу. Валидные данные.', () => {
        const validationErrors = validateValues(validationInfo, formValues);

        expect(_isEmpty(validationErrors)).toBe(true);
    });

    it('Зависимая валидация с указанием на другую группу. Невалидные данные.', () => {
        formValues.contacts.phone = '';

        const validationErrors = validateValues(validationInfo, formValues);

        expect(_isEmpty(validationErrors)).toBe(false);
        expect(validationErrors.contacts.phone).toEqual({
            blurError:
                'Ошибка зависимой валидации со ссылкой (Если есть бонусная карта, то должен быть указан телефон)',
        });
    });

    it('Зависимая валидация типа Some. Невалидные данные.', () => {
        formValues.contacts.phone = 'invalidPhone';

        const validationErrors = validateValues(validationInfo, formValues);

        expect(_isEmpty(validationErrors)).toBe(false);
        expect(validationErrors.contacts.phone).toEqual({
            blurError:
                'Ошибка зависимой валидации типа Some (Хоть у одного паспорт РФ, то телефон должен начинаться с +7)',
        });
    });

    it('Зависимая валидация типа Some не применяется. Невалидные данные.', () => {
        formValues.passenger[0].document.documentType = 'ru_foreign_passport';
        formValues.contacts.phone = 'invalidPhone';

        const validationErrors = validateValues(validationInfo, formValues);

        expect(_isEmpty(validationErrors)).toBe(true);
    });

    it('Зависимая валидация типа Every. Невалидные данные.', () => {
        formValues.passenger[1].document.documentType = 'ru_passport';
        formValues.contacts.phone = '+7invalidPhone';

        const validationErrors = validateValues(validationInfo, formValues);

        expect(_isEmpty(validationErrors)).toBe(false);
        expect(validationErrors.contacts.phone).toEqual({
            blurError:
                'Ошибка зависимой валидации типа Every (Хоть у всех пассажиров паспорт РФ, то телефон должен начинаться с +7900)',
        });
    });

    it('Зависимая валидация типа Every не применяется. Невалидные данные.', () => {
        formValues.contacts.phone = '+7invalidPhone';

        const validationErrors = validateValues(validationInfo, formValues);

        expect(_isEmpty(validationErrors)).toBe(true);
    });
});
