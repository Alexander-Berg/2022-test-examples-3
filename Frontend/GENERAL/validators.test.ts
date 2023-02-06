import { validateEmptiness, validateName, validateSlug, validateTags } from './validators';
import { AbcBackendError } from '~/src/common/fetcher/AbcBackendError';

jest.mock('../redux/ServiceCreation.api', () => (
    {
        requestSlugValidation: async(slug: string) => {
            if (!slug) {
                throw new AbcBackendError({
                    data: { detail: 'error detail', message: { ru: 'error message', en: 'err' } },
                    status: 200,
                });
            }

            if (slug.length > 50) {
                throw new AbcBackendError({ data: { detail: 'error detail' }, status: 200 });
            }

            if (slug === 'abc') {
                return Promise.resolve({ valid: false });
            }

            return Promise.resolve({ valid: true });
        },
        requestTagsValidation: async(tags: string[], _parent?: number) => {
            if (tags[0] === 'vs' && tags[1] === 'bu') {
                throw new AbcBackendError({
                    data: { detail: 'error detail', message: { ru: 'error message', en: 'err' } },
                    status: 200,
                });
            }

            if (tags.length === 0) {
                throw new AbcBackendError({ data: { detail: 'error detail' }, status: 200 });
            }

            return Promise.resolve({ valid: true });
        },
    }
));

describe('validators', () => {
    const mainStepDataMock = {
        name: '',
        englishName: '',
        slug: '',
        owner: undefined,
        parent: undefined,
        tags: [],
    };
    describe('validateEmptiness', () => {
        const validateDescriptionEmptiness = validateEmptiness('description');
        it('Should return error with empty string passed', async() => {
            const result = await validateDescriptionEmptiness({ description: '', englishDescription: '' });

            expect(result).toEqual({
                passed: false,
                error: 'i18n:should-be-non-empty',
            });
        });

        it('Should return without error with non-empty string passed', async() => {
            const result = await validateDescriptionEmptiness({ description: 'something', englishDescription: '' });

            expect(result).toEqual({ passed: true });
        });

        it('Should return error with undefined passed', async() => {
            const result = await validateEmptiness('owner')({ ...mainStepDataMock, owner: undefined });

            expect(result).toEqual({
                passed: false,
                error: 'i18n:should-be-non-empty',
            });
        });

        it('Should return error with empty array passed', async() => {
            // @ts-ignore присваиваем полю неправильный тип, чтобы проверить валидность работы функции с массивами
            const result = await validateDescriptionEmptiness({ description: [] });

            expect(result).toEqual({
                passed: false,
                error: 'i18n:should-be-non-empty',
            });
        });

        it('Should return without error with object passed', async() => {
            const result = await validateEmptiness('owner')({ ...mainStepDataMock, owner: { id: '1', title: 'value' } });

            expect(result).toEqual({ passed: true });
        });
    });

    describe('validateSlug', () => {
        it('Should return error message when request return error with detail and message', async() => {
            const result = await validateSlug({ ...mainStepDataMock, slug: '' });

            expect(result).toEqual({
                passed: false,
                error: 'error message',
            });
        });

        it('Should return error detail when request return error with only detail', async() => {
            const result = await validateSlug({ ...mainStepDataMock, slug: 'a'.repeat(100) });

            expect(result).toEqual({
                passed: false,
                error: 'error detail',
            });
        });

        it('Should return slag busy error when request return without error with valid=false', async() => {
            const result = await validateSlug({ ...mainStepDataMock, slug: 'abc' });

            expect(result).toEqual({
                passed: false,
                error: 'i18n:slag-already-in-use',
            });
        });

        it('Should return without error', async() => {
            const result = await validateSlug({ ...mainStepDataMock, slug: 'slug' });

            expect(result).toEqual({ passed: true });
        });
    });

    describe('validateName', () => {
        it('Should return error with empty string passed', async() => {
            const result = await validateName('name')({ ...mainStepDataMock, name: '' });

            expect(result).toEqual({
                passed: false,
                error: 'i18n:should-be-non-empty',
            });
        });

        it('Should return error with 128-chars string passed', async() => {
            const result = await validateName('name')({ ...mainStepDataMock, name: 'abc'.padEnd(128, 'abc') });

            expect(result).toEqual({
                passed: false,
                error: 'i18n:should-be-less-than',
            });
        });

        it('Should return without error with 127-chars string passed', async() => {
            const result = await validateName('name')({ ...mainStepDataMock, name: 'abc'.padEnd(127, 'abc') });

            expect(result).toEqual({ passed: true });
        });
    });

    describe('validateTags', () => {
        const mockTags = [
            { id: 1, name: 'vs', color: 'red' },
            { id: 2, name: 'bu', color: 'pink' },
        ];
        it('Should return error message when request return error with detail and message', async() => {
            const result = await validateTags({ ...mainStepDataMock, tags: mockTags });

            expect(result).toEqual({
                passed: false,
                error: 'error message',
            });
        });

        it('Should return error detail when request return error with only detail', async() => {
            const result = await validateTags({ ...mainStepDataMock, tags: [] });

            expect(result).toEqual({
                passed: false,
                error: 'error detail',
            });
        });

        it('Should return without error', async() => {
            const result = await validateTags({ ...mainStepDataMock, tags: [mockTags[1]] });

            expect(result).toEqual({ passed: true });
        });
    });
});
