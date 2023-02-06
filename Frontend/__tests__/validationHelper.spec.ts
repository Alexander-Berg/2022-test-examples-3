import { createValidationObject, validateValue, validateAll } from '../validationHelper';
import { ru as lang } from '../LcForm.i18n';

describe('Validation helper', () => {
    const validatePhone = (phone: string): string[] => validateValue(phone, { phone: true }, lang);
    const validateEmail = (email: string): string[] => validateValue(email, { email: true }, lang);
    const validateMaxLength = (value: string): string[] => validateValue(value, { 'max-length': 10 }, lang);

    it('create validation object', () => {
        const children = [
            {
                props: {
                    name: 'phone',
                    validation: {
                        required: true,
                        phone: true,
                    },
                },
            },
            56,
            {
                props: {
                    name: 'email',
                    validation: {
                        email: true,
                    },
                },
            },
            'br',
            {
                props: {
                    name: 'agreement',
                },
            },
        ];

        expect(createValidationObject(children)).toEqual({
            phone: {
                required: true,
                phone: true,
            },
            email: {
                email: true,
            },
        });
    });

    it('should filter list with non exist validation', () => {
        const validationObject = {
            abrakadabra: true,
            dfdfdfdf: true,
            required: true,
        };

        expect(validateValue('any value', validationObject, lang)).toEqual([]);
    });

    describe('Should pass validation on these phones:', () => {
        const validPhones = [
            '+7 926 123 34 45',
            '+7 (926) 123 34 45',
            '+79261233445',
            '89261233445',
            '8 926 123 34 45',
            '+72 926 123 34 45',
            '723 926 123 34 45',
            '+7 (926) 123-34-45',
            '745 (926) 123-34-45',
            '+7459261233445',
        ];

        validPhones.forEach(phone => it(phone, () => {
            expect(validatePhone(phone)).toEqual([]);
        }));
    });

    describe('Should fail validation on these phones:', () => {
        const invalidPhones = [
            ' +7 (926) 123-34-45 ',
            '745-(926) 123-34-45',
            '7451 (926) 123-34-45',
            '+7 (926) 123-34-4',
            '+7 (926) 123dfdf34-4',
            '+7 (926) 123---34-4',
            '+7 (926) 1234-34-44',
            '+7 (92) 123-34-4',
            '+(723) (926)-123-34-4',
            '+7 926 123_34_44',
            '+7 926     123_34_44',
        ];

        invalidPhones.forEach(phone => it(phone, () => {
            expect(validatePhone(phone)).toEqual(['Номер телефона указан неверно']);
        }));
    });

    describe('Should pass validation on these emails:', () => {
        const validEmails = [
            'fgf@gmail.com',
            'sd@sss.com',
            'abrakadabra@big-domainblablabla.com',
        ];

        validEmails.forEach(email => it(email, () => {
            expect(validateEmail(email)).toEqual([]);
        }));
    });

    describe('Should fail validation on these emails:', () => {
        const invalidEmails = [
            '@gmail.com',
            'fgf@.com',
            'fgf@',
            'dfdfdfdf',
            '@@@@',
        ];

        invalidEmails.forEach(email => it(email, () => {
            expect(validateEmail(email)).toEqual(['Адрес электронной почты должен быть в формате author@example.com']);
        }));
    });

    it('should pass validation with max-length 10 on 1234567890', () => {
        expect(validateMaxLength('1234567890')).toEqual([]);
    });

    it('should fail validation with max-length 10 on 12345678900', () => {
        expect(validateMaxLength('12345678900')).toEqual(['Максимальное количество символов в поле 10']);
    });

    it('should validate all fields', () => {
        const values = {
            phone: 'aå',
            email: 'dfdf@gmail.com',
            requiredSelect1: 'any value',
            requiredSelect2: '',
            optional: '',
        };

        const validations = {
            phone: { required: true, phone: true },
            email: { required: true, email: true },
            requiredSelect1: { required: true },
            requiredSelect2: { required: true },
        };

        expect(validateAll(values, validations, lang)).toEqual({
            requiredSelect1: [],
            email: [],
            phone: ['Номер телефона указан неверно'],
            requiredSelect2: ['Обязательное для заполнения поле'],
        });
    });
});
