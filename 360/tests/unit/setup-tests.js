import { configure } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import fs from 'fs';
import path from 'path';

configure({ adapter: new Adapter() });

require('./globals');

jest.mock('./.storybook/facade');

// мок во избежание `Error: Not implemented: HTMLMediaElement.prototype.pause`
HTMLMediaElement.prototype.pause = () => Promise.resolve();

const mocks = {};
const LEGO_COMPONENTS_MOCK_DIR = '../../node_modules/@ps-int/ufo-rocks/_lego-components-mocks_/';
fs.readdirSync(path.resolve(__dirname, LEGO_COMPONENTS_MOCK_DIR)).forEach((mock) => {
    const mockDir = mock.replace('.js', '');

    // В web-client есть тест, который завязан на реализацию Textinput, поэтому для него не создаем mock
    if (mockDir !== 'Textinput') {
        mocks['../../node_modules/@ps-int/ufo-rocks/lib/components/lego-components/' + mockDir] = LEGO_COMPONENTS_MOCK_DIR + mock;
    }
});

Object.keys(mocks).forEach((mockPath) => {
    jest.mock(mockPath, () => require(mocks[mockPath]));
});
