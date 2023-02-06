import {ApiError} from 'bla';
import {is404ApiError} from '../is404ApiError';

describe('is404ApiError', () => {
    it('Если ошибка не является инстансом ApiError - вернём false', () => {
        expect(is404ApiError(new Error('fail'), ApiError)).toBe(false);
    });

    it('Если ошибка не связана с 404 - вернём false', () => {
        expect(
            is404ApiError(
                new ApiError(ApiError.BAD_REQUEST, 'invalid params'),
                ApiError,
            ),
        ).toBe(false);
    });

    it('Если тип ошибки NOT_FOUND - вернём true', () => {
        expect(
            is404ApiError(
                new ApiError(ApiError.NOT_FOUND, 'no results'),
                ApiError,
            ),
        ).toBe(true);
    });

    it('Если в сообщении ошибки есть упоминание 404 - интерпретируем как 404 и возвращаем true', () => {
        expect(
            is404ApiError(
                new ApiError(
                    ApiError.INTERNAL_ERROR,
                    'Response code 404 (Not Found)',
                ),
                ApiError,
            ),
        ).toBe(true);
    });
});
