import Error from '../../../../../src/components/error';

jest.mock('../../../../../src/lib/metrika');
jest.mock('@ps-int/ufo-rocks/lib/components/overdraft-content', () => () => null);

import { runTest } from './common';

describe('error =>', () => {
    it('заблокированный файл', () => {
        runTest(Error, {
            blocked: true
        });
    });

    it('битая ссылка или распубликованный файл', () => {
        runTest(Error, {
            errorCode: 404
        });
    });

    it('ошибка сервера', () => {
        runTest(Error, {
            errorCode: 500
        });
    });

    it('без рекламы (платный пользователь)', () => {
        runTest(Error, {
            errorCode: 500,
            noAdv: true
        });
    });
});
