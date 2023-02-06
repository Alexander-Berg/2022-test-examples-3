import 'jest-enzyme';
import Enzyme from 'enzyme';
import EnzymeAdapterReact from 'enzyme-adapter-react-16';

Enzyme.configure({ adapter: new EnzymeAdapterReact() });

import { setTankerProjectId, addTranslation } from 'react-tanker';
require('@ps-int/ufo-rocks/setup');

setTankerProjectId('yandex_disk_web');
addTranslation('ru', require('../../i18n/loc/ru'));

process.env.YANDEX_ENVIRONMENT = process.env.YANDEX_ENVIRONMENT || 'development';
process.env.NODE_ENV = process.env.NODE_ENV || 'development';

global.APP = false;

global.popFnCalls = (jestFn) => {
    const calls = jestFn.mock.calls.slice();
    jestFn.mockClear();

    return calls;
};

// мок во избежание `Error: Not implemented: HTMLMediaElement.prototype.pause`
HTMLMediaElement.prototype.pause = () => Promise.resolve();
