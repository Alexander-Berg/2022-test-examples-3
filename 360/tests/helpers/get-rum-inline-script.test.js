jest.mock('fs');
require('fs').__setMockFiles({
    'node_modules/@yandex-int/rum-counter/dist/inline/interface.min.js': 'interface.min.js',
    'node_modules/@yandex-int/rum-counter/dist/inline/longtask.min.js': 'longtask.min.js',
    'node_modules/@yandex-int/rum-counter/dist/bundle/send.min.js': 'send.min.js',
    'node_modules/@yandex-int/error-counter/dist/interfaceOverRum.min.js': 'interfaceOverRum.min.js',
    'node_modules/@yandex-int/error-counter/dist/implementation.min.js': 'implementation.min.js',
    'node_modules/@yandex-int/error-counter/dist/filters.min.js': 'filters.min.js',
    'node_modules/@yandex-int/error-counter/dist/logError.min.js': 'logError.min.js',
});
const { getRumInlineScript, IDS } = require('../../helpers/rum');

describe('getRumInlineScript', () => {
    it('должен корректно генерировать инлайн скрипт', () => {
        const script = getRumInlineScript({
            requestId: '123456',
            regionId: '9999',
            ua: {}
        }, [IDS.RU, IDS.YANDEX_DISK, 3], {
            projectName: 'disk-test',
            page: 'index',
            platform: 'desktop',
            version: '1.0.0'
        }, ['12345,0,0', '54321,0,0']);

        // eslint-disable max-len
        const expected = `
interface.min.js
longtask.min.js
send.min.js
interfaceOverRum.min.js
implementation.min.js
filters.min.js
logError.min.js
Ya.Rum.init({beacon:true,sendClientUa:true,sendAutoResTiming:true,sendAutoElementTiming:true,clck:'https://yandex.ru/clck/click',slots:["12345,0,0","54321,0,0"],reqid:'123456'},{'287':'9999','143':'28.1008.3','-project':'disk-test','-page':'index','-env':'test','-platform':'desktop'});
Ya.Rum.initErrors({project:'disk-test',page:'index',env:'test',platform:'desktop',version:'1.0.0',unhandledRejection:true,debug:false,experiments:[]});
`;
        expect(script).toEqual(expected);
    });
});
