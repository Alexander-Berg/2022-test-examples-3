import { mockAdapterContext } from '@yandex-turbo/applications/beru.ru/mocks/adapterContext';
import { mockRRContext } from '@yandex-turbo/applications/beru.ru/mocks/report-renderer';
import { IrrCtx } from '@yandex-turbo/types/rrCtx';
import { IAdapterContext } from '@yandex-turbo/types/AdapterContext';
import * as methods from '../adapter-rrCtx';

type TSpyFn = ReturnType<typeof jest.spyOn>;

function getAjaxShape(value: unknown, ctxType: 'rr' | 'adapter') {
    return {
        [ctxType === 'rr' ? 'parsedBody' : 'data']: {
            cgidata: {
                args: { ajax: [value] },
            },
        },
    };
}

describe('getCgiData', () => {
    it('должна возвращать cgi параметры из RR контекста', () => {
        const rrCtx = mockRRContext();

        expect(methods.getCgiData(rrCtx)).toEqual(rrCtx.parsedBody.cgidata);
    });

    it('должна возвращать cgi параметры из контекста адаптера', () => {
        const adapterCtx = mockAdapterContext();

        expect(methods.getCgiData(adapterCtx)).toEqual(adapterCtx.data.cgidata);
    });
});

describe('getQueryParams', () => {
    it('должна возвращать query-params из RR контекста', () => {
        const rrCtx = mockRRContext();

        expect(methods.getQueryParams(rrCtx)).toEqual(rrCtx.parsedBody.cgidata.args);
    });

    it('должна возвращать query-params из контекста адаптера', () => {
        const adapterCtx = mockAdapterContext();

        expect(methods.getQueryParams(adapterCtx)).toEqual(adapterCtx.data.cgidata.args);
    });
});

describe('getServiceURL', () => {
    it('должна возвращать url приложения из RR контекста', () => {
        const rrCtx = mockRRContext();

        expect(methods.getServiceURL(rrCtx)).toEqual(rrCtx.parsedBody.cgidata.args.text[0]);
    });

    it('должна возвращать url приложения из контекста адаптера', () => {
        const adapterCtx = mockAdapterContext();

        expect(methods.getServiceURL(adapterCtx)).toEqual(adapterCtx.data.cgidata.args.text[0]);
    });
});

describe('isAjax', () => {
    it('должна правильно определять признак ajax запроса из RR контекста', () => {
        // проверяем на истинном занчении, все остальные должны давать false
        const type = 'rr';
        const { isAjax } = methods;
        let rrCtx = mockRRContext(getAjaxShape('1', type));

        expect(isAjax(rrCtx)).toEqual(true);

        rrCtx = mockRRContext(getAjaxShape(1, type));
        expect(isAjax(rrCtx)).toEqual(false);

        rrCtx = mockRRContext(getAjaxShape('', type));
        expect(isAjax(rrCtx)).toEqual(false);

        rrCtx = mockRRContext(getAjaxShape(null, type));
        expect(isAjax(rrCtx)).toEqual(false);

        rrCtx = mockRRContext(getAjaxShape(undefined, type));
        expect(isAjax(rrCtx)).toEqual(false);

        rrCtx = mockRRContext(getAjaxShape({}, type));
        expect(isAjax(rrCtx)).toEqual(false);

        rrCtx = mockRRContext(getAjaxShape([], type));
        expect(isAjax(rrCtx)).toEqual(false);
    });

    it('должна правильно определять признак ajax запроса из адаптер контекста', () => {
        // проверяем на истинном занчении, все остальные должны давать false
        const type = 'adapter';
        const { isAjax } = methods;
        let adapterCtx = mockAdapterContext(getAjaxShape('1', type));

        expect(isAjax(adapterCtx)).toEqual(true);

        adapterCtx = mockAdapterContext(getAjaxShape(1, type));
        expect(isAjax(adapterCtx)).toEqual(false);

        adapterCtx = mockAdapterContext(getAjaxShape('', type));
        expect(isAjax(adapterCtx)).toEqual(false);

        adapterCtx = mockAdapterContext(getAjaxShape(null, type));
        expect(isAjax(adapterCtx)).toEqual(false);

        adapterCtx = mockAdapterContext(getAjaxShape(undefined, type));
        expect(isAjax(adapterCtx)).toEqual(false);

        adapterCtx = mockAdapterContext(getAjaxShape({}, type));
        expect(isAjax(adapterCtx)).toEqual(false);

        adapterCtx = mockAdapterContext(getAjaxShape([], type));
        expect(isAjax(adapterCtx)).toEqual(false);
    });
});

describe('getProtocol', () => {
    describe('RR контекст', () => {
        let rrCtx: IrrCtx;

        beforeEach(() => {
            rrCtx = mockRRContext({
                parsedBody: {
                    cgidata: {
                        scheme: 'https',
                    },
                },
            });
        });

        it('должна возвращать протокол = http в development окружении', () => {
            process.env.NODE_ENV = 'development';
            expect(methods.getProtocol(rrCtx)).toEqual('http');
        });

        it('должна возвращать протокол из контекста для окруженяи отличного от development', () => {
            const { getProtocol } = methods;

            process.env.NODE_ENV = 'production';
            expect(getProtocol(rrCtx)).toEqual('https');

            process.env.NODE_ENV = 'testing';
            expect(getProtocol(rrCtx)).toEqual('https');
        });
    });

    describe('Контекст адаптера', () => {
        let adapterCtx: IAdapterContext;

        beforeEach(() => {
            adapterCtx = mockAdapterContext({
                data: {
                    cgidata: {
                        scheme: 'https',
                    },
                },
            });
        });

        it('должна возвращать протокол = http в development окружении', () => {
            process.env.NODE_ENV = 'development';
            expect(methods.getProtocol(adapterCtx)).toEqual('http');
        });

        it('должна возвращать протокол из контекста для окруженяи отличного от development', () => {
            const { getProtocol } = methods;

            process.env.NODE_ENV = 'production';
            expect(getProtocol(adapterCtx)).toEqual('https');

            process.env.NODE_ENV = 'testing';
            expect(getProtocol(adapterCtx)).toEqual('https');
        });
    });
});

describe('makeTurboUrl', () => {
    let getProtocol: TSpyFn;

    beforeEach(() => {
        getProtocol = jest.spyOn(methods, 'getProtocol').mockReturnValue('udp');
    });

    afterEach(() => {
        getProtocol.mockReset();
    });

    it('должна возвращать корректно сформированный url из RR контекста', () => {
        const rrCtx = mockRRContext();
        const serviceUrl = 'https://project.com/path/to';

        // Проверочные данные для host и pathname берутся из основного мока.
        expect(methods.makeTurboUrl(rrCtx, serviceUrl)).toEqual(`udp://localhost:3333/turbo?text=${encodeURIComponent(serviceUrl)}`);
        expect(getProtocol).toHaveBeenCalledTimes(1);
    });

    it('должна возвращать корректно сформированный url из контекста адаптера', () => {
        const adapterCtx = mockAdapterContext();
        const serviceUrl = 'https://project.com/path/to';

        expect(methods.makeTurboUrl(adapterCtx, serviceUrl)).toEqual(`udp://localhost:3333/turbo?text=${encodeURIComponent(serviceUrl)}`);
        expect(getProtocol).toHaveBeenCalledTimes(1);
    });
});

describe('isExample', () => {
    it('возвращает bool значение флага isExampleData, если вызван в контексте адаптера', () => {
        const ctx = { data: { doc: { isExampleData: true } } };

        expect(methods.isExample(mockAdapterContext(ctx))).toEqual(true);

        ctx.data.doc.isExampleData = false;

        expect(methods.isExample(mockAdapterContext(ctx))).toEqual(false);

        delete ctx.data.doc.isExampleData;

        expect(methods.isExample(mockAdapterContext(ctx))).toEqual(false);
    });

    it('возвращает всегда false, если вызван не в контексте адаптера', () => {
        expect(methods.isExample(mockRRContext())).toEqual(false);
    });
});

describe('getUnparsedURI', () => {
    it('коррктно возвращает unparsed_uri из rr и adapter контекстов', () => {
        const rrCtx = mockRRContext();
        const adapterCtx = mockAdapterContext();

        expect(methods.getUnparsedURI(rrCtx)).toEqual(rrCtx.parsedBody.reqdata.unparsed_uri);
        expect(methods.getUnparsedURI(adapterCtx)).toEqual(adapterCtx.data.reqdata.unparsed_uri);
    });
});

describe('getRequestUrl', () => {
    let getProtocol: TSpyFn;

    beforeEach(() => {
        getProtocol = jest.spyOn(methods, 'getProtocol').mockReturnValue('udp');
    });

    afterEach(() => {
        getProtocol.mockReset();
    });

    it('возвращает запрашиваемый клиентом url', () => {
        const rrCtx = mockRRContext();
        const adapterCtx = mockAdapterContext();

        expect(methods.getRequestUrl(rrCtx)).toEqual(
            `udp://${rrCtx.parsedBody.cgidata.hostname}${rrCtx.parsedBody.reqdata.unparsed_uri}`
        );
        expect(methods.getRequestUrl(adapterCtx)).toEqual(
            `udp://${adapterCtx.data.cgidata.hostname}${adapterCtx.data.reqdata.unparsed_uri}`
        );
    });
});
