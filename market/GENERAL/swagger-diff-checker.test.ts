import fetch from 'node-fetch';
import {leftSpec} from './test/light-specs/left';
import {rightSpec} from './test/light-specs/right';
import {swaggerDiffChecker, IBreakingChangesNames} from './swagger-diff-checker';
import {fetchConfig} from './utils/fetch-swagger';

jest.mock('node-fetch');

(fetch as any).mockImplementation((url: string) => ({
    json: () => {
        const lowerUrl = url.toLocaleLowerCase();
        const useLeftSpec =
            // mocked value
            lowerUrl.includes('left')
            // default url, see config
            || lowerUrl.includes('38271');
        return useLeftSpec ? leftSpec : rightSpec;
    }
}));


describe('main', () => {
    const ORIGIN_ENV = process.env;

    beforeEach(() => {
        jest.resetModules();
        process.env = {
            ...ORIGIN_ENV,
            SWAGGER_SPEC_URL_LEFT: 'http://spec.ru/left',
            SWAGGER_SPEC_URL_RIGHT: 'http://spec.ru/right',
        };
        (fetch as any).mockClear();
    });

    afterEach(() => {
        process.env = ORIGIN_ENV;
    });

    // При expect(fetch) метод toHaveBeenNthCalledWith имеет сквозную нумерацию вызовов fetch!
    it('should use env variables to get swagger specs', async () => {
        const report = await swaggerDiffChecker();

        expect(fetch).toHaveBeenNthCalledWith(1, 'http://spec.ru/left', fetchConfig);
        expect(fetch).toHaveBeenNthCalledWith(2, 'http://spec.ru/right', fetchConfig);
        expect(report).toEqual([
            'В get /parameter-became-renamed параметр old-name стал обязательным.',
            'В get-ручке /parameter-became-required параметр requiredParam был переименован из false в true.',
            "В get-ручке /parameter-gets-moved параметр whateverId был перенесён из query в body",
            "В get-ручке /parameter-type-is-going-to-change тип параметра hyperId был изменен с string на number",
            'В get-ручке /parameter-was-removed удалён параметр to-be-removed.',
            "В ответе 200 ручки get /break-array-dto изменен формат ответа: изменено поле responseC_field1 в типе responseC. Зависимость ручки от типа responseC: тип Array -> поле data в типе ArrayItem",
            "В ответе 200 ручки get /break-object-dto изменен формат ответа: изменено поле responseC_field1 в типе responseC. Зависимость ручки от типа responseC: поле responseA_propertyB в типе responseA -> поле responseB_propertyC в типе responseB",
            "В ручке get /new-required-parameter добавлен обязательный параметр requiredParam",
            "В ручке get /parameter-became-renamed добавлен обязательный параметр new-name-2",
            "В ручке get /parameter-became-renamed добавлен обязательный параметр new-name-4",
            "Ручка get /an-old-one была удалена или переименована",
            "Ручка get /path-gonna-be-removed была удалена или переименована",
            "Ручка post /path-gonna-be-removed была удалена или переименована",
        ]);
    });

    it('should use default values to get swagger specs if env variables not provided', async () => {
        process.env.SWAGGER_SPEC_URL_LEFT = '';
        process.env.SWAGGER_SPEC_URL_RIGHT = '';

        const report = await swaggerDiffChecker();

        expect(fetch).toHaveBeenNthCalledWith(1,
            'http://mbi-partner.tst.vs.market.yandex.net:38271/v2/api-docs',
            fetchConfig
        );
        expect(fetch).toHaveBeenNthCalledWith(2,
            'https://raw.github.yandex-team.ru/market/partner-swagger-versions/master/swag.json',
            fetchConfig
        );
        expect(report).toEqual([
            'В get /parameter-became-renamed параметр old-name стал обязательным.',
            'В get-ручке /parameter-became-required параметр requiredParam был переименован из false в true.',
            "В get-ручке /parameter-gets-moved параметр whateverId был перенесён из query в body",
            "В get-ручке /parameter-type-is-going-to-change тип параметра hyperId был изменен с string на number",
            'В get-ручке /parameter-was-removed удалён параметр to-be-removed.',
            "В ответе 200 ручки get /break-array-dto изменен формат ответа: изменено поле responseC_field1 в типе responseC. Зависимость ручки от типа responseC: тип Array -> поле data в типе ArrayItem",
            "В ответе 200 ручки get /break-object-dto изменен формат ответа: изменено поле responseC_field1 в типе responseC. Зависимость ручки от типа responseC: поле responseA_propertyB в типе responseA -> поле responseB_propertyC в типе responseB",
            "В ручке get /new-required-parameter добавлен обязательный параметр requiredParam",
            "В ручке get /parameter-became-renamed добавлен обязательный параметр new-name-2",
            "В ручке get /parameter-became-renamed добавлен обязательный параметр new-name-4",
            "Ручка get /an-old-one была удалена или переименована",
            "Ручка get /path-gonna-be-removed была удалена или переименована",
            "Ручка post /path-gonna-be-removed была удалена или переименована",
        ]);
    });

    it('should avoid checks provided within EXCLUDED_CHECKS env', async () => {
        process.env.EXCLUDED_CHECKS = 'parameter-was-removed,parameter-became-required';

        const report = await swaggerDiffChecker();

        expect(fetch).toHaveBeenNthCalledWith(1, 'http://spec.ru/left', fetchConfig);
        expect(fetch).toHaveBeenNthCalledWith(2, 'http://spec.ru/right', fetchConfig);
        expect(report).toEqual([
            'В get /parameter-became-renamed параметр old-name стал обязательным.',
            "В get-ручке /parameter-gets-moved параметр whateverId был перенесён из query в body",
            "В get-ручке /parameter-type-is-going-to-change тип параметра hyperId был изменен с string на number",
            "В ответе 200 ручки get /break-array-dto изменен формат ответа: изменено поле responseC_field1 в типе responseC. Зависимость ручки от типа responseC: тип Array -> поле data в типе ArrayItem",
            "В ответе 200 ручки get /break-object-dto изменен формат ответа: изменено поле responseC_field1 в типе responseC. Зависимость ручки от типа responseC: поле responseA_propertyB в типе responseA -> поле responseB_propertyC в типе responseB",
            "В ручке get /new-required-parameter добавлен обязательный параметр requiredParam",
            "В ручке get /parameter-became-renamed добавлен обязательный параметр new-name-2",
            "В ручке get /parameter-became-renamed добавлен обязательный параметр new-name-4",
            "Ручка get /an-old-one была удалена или переименована",
            "Ручка get /path-gonna-be-removed была удалена или переименована",
            "Ручка post /path-gonna-be-removed была удалена или переименована",
        ]);
    });

    it('should use excluded checks list and specs urls from parameter', async () => {
        const leftSpecUrl = 'left-url';
        const rightSpecUrl = 'right-url';
        const excludedChecks: IBreakingChangesNames[] = ['parameter-was-removed', 'parameter-became-required'];

        const report = await swaggerDiffChecker({leftSpecUrl, rightSpecUrl, excludedChecks});

        expect(fetch).toHaveBeenNthCalledWith(1, leftSpecUrl, fetchConfig);
        expect(fetch).toHaveBeenNthCalledWith(2, rightSpecUrl, fetchConfig);
        expect(report).toEqual([
            'В get /parameter-became-renamed параметр old-name стал обязательным.',
            "В get-ручке /parameter-gets-moved параметр whateverId был перенесён из query в body",
            "В get-ручке /parameter-type-is-going-to-change тип параметра hyperId был изменен с string на number",
            "В ответе 200 ручки get /break-array-dto изменен формат ответа: изменено поле responseC_field1 в типе responseC. Зависимость ручки от типа responseC: тип Array -> поле data в типе ArrayItem",
            "В ответе 200 ручки get /break-object-dto изменен формат ответа: изменено поле responseC_field1 в типе responseC. Зависимость ручки от типа responseC: поле responseA_propertyB в типе responseA -> поле responseB_propertyC в типе responseB",
            "В ручке get /new-required-parameter добавлен обязательный параметр requiredParam",
            "В ручке get /parameter-became-renamed добавлен обязательный параметр new-name-2",
            "В ручке get /parameter-became-renamed добавлен обязательный параметр new-name-4",
            "Ручка get /an-old-one была удалена или переименована",
            "Ручка get /path-gonna-be-removed была удалена или переименована",
            "Ручка post /path-gonna-be-removed была удалена или переименована",
        ]);
    });

    it('should use specs from parameter', async () => {
        const excludedChecks: IBreakingChangesNames[] = ['parameter-was-removed', 'parameter-became-required'];

        const report = await swaggerDiffChecker({leftSpec: leftSpec, rightSpec: rightSpec, excludedChecks});

        expect(fetch).not.toHaveBeenCalled();
        expect(report).toEqual([
            'В get /parameter-became-renamed параметр old-name стал обязательным.',
            "В get-ручке /parameter-gets-moved параметр whateverId был перенесён из query в body",
            "В get-ручке /parameter-type-is-going-to-change тип параметра hyperId был изменен с string на number",
            "В ответе 200 ручки get /break-array-dto изменен формат ответа: изменено поле responseC_field1 в типе responseC. Зависимость ручки от типа responseC: тип Array -> поле data в типе ArrayItem",
            "В ответе 200 ручки get /break-object-dto изменен формат ответа: изменено поле responseC_field1 в типе responseC. Зависимость ручки от типа responseC: поле responseA_propertyB в типе responseA -> поле responseB_propertyC в типе responseB",
            "В ручке get /new-required-parameter добавлен обязательный параметр requiredParam",
            "В ручке get /parameter-became-renamed добавлен обязательный параметр new-name-2",
            "В ручке get /parameter-became-renamed добавлен обязательный параметр new-name-4",
            "Ручка get /an-old-one была удалена или переименована",
            "Ручка get /path-gonna-be-removed была удалена или переименована",
            "Ручка post /path-gonna-be-removed была удалена или переименована",
        ]);
    });
});
