import 'jest-enzyme';
import Enzyme from 'enzyme';
import EnzymeAdapterReact from 'enzyme-adapter-react-16';
import path from 'path';
import fs from 'fs';

Enzyme.configure({ adapter: new EnzymeAdapterReact() });

import { setTankerProjectId, addTranslation } from 'react-tanker';
require('@ps-int/ufo-rocks/setup');

setTankerProjectId('yandex_disk_web');
addTranslation('ru', require('../../i18n/loc/ru'));

global.popFnCalls = (jestFn) => {
    const calls = jestFn.mock.calls.slice();
    jestFn.mockClear();

    return calls;
};

const mocks = {};
const LEGO_MOCK_DIR = '../../_lego-mocks_/';
fs.readdirSync(path.resolve(__dirname, LEGO_MOCK_DIR)).forEach((mock) => {
    const mockDir = mock.replace('.js', '');
    const componentsDir = mockDir[0] === mockDir[0].toUpperCase() ? 'lego-components' : 'lego-on-react';
    mocks[`../../node_modules/@ps-int/ufo-rocks/lib/components/${componentsDir}/${mockDir}`] = LEGO_MOCK_DIR + mock;
});

const LEGO_COMPONENTS_MOCK_DIR = '../../node_modules/@ps-int/ufo-rocks/_lego-components-mocks_/';
fs.readdirSync(path.resolve(__dirname, LEGO_COMPONENTS_MOCK_DIR)).forEach((mock) => {
    const mockDir = mock.replace('.js', '');
    mocks['../../node_modules/@ps-int/ufo-rocks/lib/components/lego-components/' + mockDir] = LEGO_COMPONENTS_MOCK_DIR + mock;
});

Object.keys(mocks).forEach((mockPath) => {
    jest.mock(mockPath, () => require(mocks[mockPath]));
});
