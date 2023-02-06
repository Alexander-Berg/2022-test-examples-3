import { runTest } from './common';

jest.mock('../../../../../src/lib/metrika');
jest.mock('@ps-int/ufo-rocks/lib/components/overdraft-content', () => () => null);

describe('error (APP) =>', () => {
    beforeEach(() => {
        global.APP = true;
    });
    afterEach(() => {
        global.APP = false;
    });

    it('в приложении не должно быть рекламы', () => {
        // чтобы получить компонент "для приложения" - надо подтягивать зависимость после установки APP=true
        const ErrorApp = require('../../../../../src/components/error').default;
        runTest(ErrorApp, {
            blocked: true
        });
    });
});
