import _isEmpty from 'lodash/isEmpty';
import _cloneDeep from 'lodash/cloneDeep';

import {IFieldError} from 'types/common/validation/form';

import {
    arrayFormFalues,
    baseFormValidationInfo,
} from './__mocks__/baseFormValues';
import validateValues from '../validateValues';

const baseBlurError: IFieldError = {blurError: 'Ошибка'};
const dependentBlurError: IFieldError = {
    blurError: 'Ошибка зависимой валидации',
};

describe('Валидация данных формы в массиве', () => {
    let formValues = arrayFormFalues;
    let validationInfo = baseFormValidationInfo;

    beforeEach(() => {
        formValues = _cloneDeep(arrayFormFalues);
        validationInfo = _cloneDeep(baseFormValidationInfo);
    });

    it('Простая и зависимая, вложенных и не вложенных данных. Валидные данные.', () => {
        const validationErrors = validateValues(validationInfo, formValues);

        expect(_isEmpty(validationErrors)).toBe(true);
    });

    it('Простая валидация. Невалидные данные.', () => {
        formValues[0].lastName = 'invalidLatinName';
        formValues[1].lastName = 'invalidLatinName';

        const validationErrors = validateValues(validationInfo, formValues);

        expect(_isEmpty(validationErrors)).toBe(false);
        expect(validationErrors[0].lastName).toEqual(baseBlurError);
        expect(validationErrors[1].lastName).toEqual(baseBlurError);
    });

    it('Простая валидация вложенного значения. Невалидные данные.', () => {
        formValues[0].document.documentType = 'invalidDocType';

        const validationErrors = validateValues(validationInfo, formValues);

        expect(_isEmpty(validationErrors)).toBe(false);
        expect(validationErrors[0].document.documentType).toEqual(
            baseBlurError,
        );
    });

    it('Зависимая валидация применяется. Невалидные данные.', () => {
        formValues[0].lastName = 'ОченьДлиннаяКириллическаяФамилия';

        const validationErrors = validateValues(validationInfo, formValues);

        expect(_isEmpty(validationErrors)).toBe(false);
        expect(validationErrors[0].lastName).toEqual(dependentBlurError);
    });

    it('Зависимая валидация не применяется по первому условию, невалидные данные не выдают ошибку.', () => {
        const dependValidationCondiditons =
            validationInfo.fieldGroups[0].fields[0].dependentValidations?.[0]
                .conditions;

        expect(dependValidationCondiditons?.[0].value[0]).toBeDefined();

        if (dependValidationCondiditons?.[0].value[0]) {
            formValues[0].lastName = 'ОченьДлиннаяКириллическаяФамилия';
            dependValidationCondiditons[0].value[0].params = '^[A-Za-z]+$';

            const validationErrors = validateValues(validationInfo, formValues);

            expect(_isEmpty(validationErrors)).toBe(true);
        }
    });

    it('Зависимая валидация не применяется по второму условию, невалидные данные не выдают ошибку.', () => {
        const dependDocumentTypeCondition =
            validationInfo.fieldGroups[0].fields[0].dependentValidations?.[0]
                .conditions?.[1].value[0];

        expect(dependDocumentTypeCondition).toBeDefined();

        if (dependDocumentTypeCondition) {
            formValues[0].lastName = 'ОченьДлиннаяКириллическаяФамилия';
            dependDocumentTypeCondition.params = ['invalidDocType'];

            const validationErrors = validateValues(validationInfo, formValues);

            expect(_isEmpty(validationErrors)).toBe(true);
        }
    });

    it('Зависимая валидация вложенных значений. Невалидные данные.', () => {
        formValues[0].document.documentNumber = '12345678901234567890';

        const validationErrors = validateValues(validationInfo, formValues);

        expect(_isEmpty(validationErrors)).toBe(false);
        expect(validationErrors[0].document.documentNumber).toEqual(
            dependentBlurError,
        );
    });
});
