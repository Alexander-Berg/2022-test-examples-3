import 'jest-enzyme';
import Enzyme from 'enzyme';
import EnzymeAdapterReact from '@wojtekmaj/enzyme-adapter-react-17';

Enzyme.configure({ adapter: new EnzymeAdapterReact() });

global.React = require('react');
global.ReactDOM = require('react-dom');

const tanker = require('@ps-int/react-tanker');
tanker.setTankerProjectId('docviewer');
tanker.addTranslation('ru', require('i18n/loc/ru'));
tanker.addTranslation('en', require('i18n/loc/en'));
tanker.addTranslation('tr', require('i18n/loc/tr'));
tanker.addTranslation('uk', require('i18n/loc/uk'));

global.langs = ['ru', 'en', 'tr', 'uk'];

global.popFnCalls = (jestFn) => {
    const ret = jestFn.mock.calls.slice();
    jestFn.mockClear();

    return ret;
};

global.Ya = {
    Rum: {
        sendHeroElement: () => {}
    }
};

// моки для continuous scroll-а (нужен в тестах на html.js и content/index.js)
// замоканы такие цифры, при которых не должна запускаться подгрузка страниц сверху и снизу
const scrollTop = 40;
global.window.pageYOffset = scrollTop;
global.window.innerHeight = 20;
global.document.querySelector = (selector) => ({
    querySelector: () => null,
    addEventListener: () => {},
    parentNode: {
        getBoundingClientRect: () => {
            switch (selector) {
                case '.page-spin-top':
                    return {
                        top: 5 - scrollTop,
                        bottom: 15 - scrollTop
                    };
                case '.page-spin-bottom':
                    return {
                        top: 85 - scrollTop,
                        bottom: 95 - scrollTop
                    };
                default:
                    return {};
            }
        }
    }
});

jest.mock('react', () => Object.assign(
    {},
    jest.requireActual('react'),
    {
        // ignore hundreds of "Warning: useLayoutEffect does nothing on the server, because ..."
        useLayoutEffect: jest.requireActual('react').useEffect
    }
));
