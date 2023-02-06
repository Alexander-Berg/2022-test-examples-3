import {
    ALPHA_API_KEY,
    API_KEY,
    CMNT_ALPHA_ENDPOINT,
    CMNT_ALPHA_FLAG,
    CMNT_ALPHA_SCRIPT_SRC,
    CMNT_ENDPOINT,
    CMNT_SCRIPT_SRC,
    CMNT_VERSION_EXP_FLAG,
    getCommentatorParams,
} from '@yandex-turbo/applications/health/constants/comments';
import { expContext } from '@yandex-turbo/applications/health/contexts/experiments';
import { withContext } from '@yandex-turbo/applications/health/utils/context';
import * as utils from '@yandex-turbo/applications/health/utils';

describe('Health/constants/getCommentatorParams', () => {
    const defParams = {
        theme: 'mg',
        lang: 'ru',
    };

    it('возвращает результат без флага для development окружения', () => {
        const spy = jest.spyOn(utils, 'isProduction')
            .mockReturnValue(false);

        const commentatorParams = withContext(
            () => expContext.provider({})
        )(getCommentatorParams)();

        const result = {
            scriptSrc: CMNT_ALPHA_SCRIPT_SRC,
            apiKey: ALPHA_API_KEY,
            endpoint: CMNT_ALPHA_ENDPOINT,
            version: undefined,
            ...defParams,
        };

        expect(commentatorParams).toEqual(result);

        spy.mockClear();
    });

    it('возвращает результат без флага для production окружения', () => {
        const spy = jest.spyOn(utils, 'isProduction')
            .mockReturnValue(true);

        const commentatorParams = withContext(
            () => expContext.provider({})
        )(getCommentatorParams)();

        const result = {
            scriptSrc: CMNT_SCRIPT_SRC,
            apiKey: API_KEY,
            endpoint: CMNT_ENDPOINT,
            version: undefined,
            ...defParams,
        };

        expect(commentatorParams).toEqual(result);

        spy.mockClear();
    });

    it('возвращает результат с включенным флагом для production окружения', () => {
        const spy = jest.spyOn(utils, 'isProduction')
            .mockReturnValue(true);

        const experiments = {
            [CMNT_ALPHA_FLAG]: 1,
            [CMNT_VERSION_EXP_FLAG]: '1.11',
        };

        const commentatorParams = withContext(
            () => expContext.provider(experiments)
        )(getCommentatorParams)();

        const result = {
            scriptSrc: CMNT_ALPHA_SCRIPT_SRC,
            apiKey: ALPHA_API_KEY,
            endpoint: CMNT_ALPHA_ENDPOINT,
            version: '1.11',
            ...defParams,
        };

        expect(commentatorParams).toEqual(result);

        spy.mockClear();
    });

    it('возвращает результат с выключенным флагом для production окружения', () => {
        const spy = jest.spyOn(utils, 'isProduction')
            .mockReturnValue(true);

        const experiments = {
            [CMNT_ALPHA_FLAG]: 0,
        };

        const commentatorParams = withContext(
            () => expContext.provider(experiments)
        )(getCommentatorParams)();

        const result = {
            scriptSrc: CMNT_SCRIPT_SRC,
            apiKey: API_KEY,
            endpoint: CMNT_ENDPOINT,
            version: undefined,
            ...defParams,
        };

        expect(commentatorParams).toEqual(result);

        spy.mockClear();
    });
});
