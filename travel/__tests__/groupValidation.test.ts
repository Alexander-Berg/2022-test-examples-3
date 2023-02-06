import _isEmpty from 'lodash/isEmpty';
import _cloneDeep from 'lodash/cloneDeep';

import {IFieldError} from 'types/common/validation/form';

import {
    groupFormValues,
    groupFormValidationInfo,
} from './__mocks__/groupFormValues';
import validateValues from '../validateValues';

const baseBlurError: IFieldError = {blurError: 'Ошибка'};
const dependentBlurError: IFieldError = {
    blurError: 'Ошибка зависимой валидации',
};

describe('Валидация данных формы без групп и массивов', () => {
    let formValues = groupFormValues;
    let validationInfo = groupFormValidationInfo;

    beforeEach(() => {
        formValues = _cloneDeep(groupFormValues);
        validationInfo = _cloneDeep(groupFormValidationInfo);
    });

    it('Простая и зависимая, вложенных и не вложенных данных. Валидные данные.', () => {
        const validationErrors = validateValues(validationInfo, formValues);

        expect(_isEmpty(validationErrors)).toBe(true);
    });

    it('Простая валидация. Невалидные данные', () => {
        formValues.passenger[0].lastName = 'invalidLatinName';
        formValues.contacts.phone = '123456';

        const validationErrors = validateValues(validationInfo, formValues);

        expect(_isEmpty(validationErrors)).toBe(false);
        expect(validationErrors.passenger[0].lastName).toEqual(baseBlurError);
        expect(validationErrors.contacts.phone).toEqual(baseBlurError);
    });

    it('Простая валидация вложенного значения. Невалидные данные.', () => {
        formValues.passenger[0].document.documentType = 'invalidDocType';

        const validationErrors = validateValues(validationInfo, formValues);

        expect(_isEmpty(validationErrors)).toBe(false);
        expect(validationErrors.passenger[0].document.documentType).toEqual(
            baseBlurError,
        );
    });

    it('Зависимая валидация применяется. Невалидные данные.', () => {
        formValues.passenger[0].lastName = 'ОченьДлиннаяКириллическаяФамилия';

        const validationErrors = validateValues(validationInfo, formValues);

        expect(_isEmpty(validationErrors)).toBe(false);
        expect(validationErrors.passenger[0].lastName).toEqual(
            dependentBlurError,
        );
    });

    it('Зависимая валидация не применяется по первому условию, невалидные данные не выдают ошибку.', () => {
        const dependValidationCondiditons =
            validationInfo.fieldGroups[0].fields[0].dependentValidations?.[0]
                .conditions;

        expect(dependValidationCondiditons?.[0].value[0]).toBeDefined();

        if (dependValidationCondiditons?.[0].value[0]) {
            formValues.passenger[0].lastName =
                'ОченьДлиннаяКириллическаяФамилия';
            dependValidationCondiditons[0].value[0].params = '^[A-Za-z]+$';

            const validationErrors = validateValues(validationInfo, formValues);

            expect(_isEmpty(validationErrors)).toBe(true);
        }
    });

    it('Зависимая валидация вложенных значений. Невалидные данные.', () => {
        formValues.passenger[0].document.documentNumber =
            '12345678901234567890';

        const validationErrors = validateValues(validationInfo, formValues);

        expect(_isEmpty(validationErrors)).toBe(false);
        expect(validationErrors.passenger[0].document.documentNumber).toEqual(
            dependentBlurError,
        );
    });
});
