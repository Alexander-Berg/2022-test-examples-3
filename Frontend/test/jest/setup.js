import { configure } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import regeneratorRuntime from 'regenerator-runtime';
import { Settings as LuxonSettings } from 'luxon';
import fetch from 'node-fetch';

// фетчер берет параметры из глобального объекта
const csrfToken = { sk: 'jest-test' };
global.settings = csrfToken;

global.fetch = fetch;
global.regeneratorRuntime = regeneratorRuntime;

// useLayoutEffect пишет ошибки в лог при запуске тестов, потому что запускается в серверном окружении
// по функциональности useEffect аналогичен
jest.mock('react', () => ({
    ...jest.requireActual('react'),
    useLayoutEffect: jest.requireActual('react').useEffect,
}));

configure({ adapter: new Adapter() });

process.env.BEM_LANG = 'ru';

// luxon использует нативные средства форматирования: браузер или node
// node в CI и локально могут быть разных версий и комплектаций
// английская локаль должна быть консистентна везде
LuxonSettings.defaultLocale = 'en';
