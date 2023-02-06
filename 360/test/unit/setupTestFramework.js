import 'cross-fetch/polyfill';
import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import sinon from 'sinon';
import {setTankerProjectId, addTranslation} from 'react-tanker';

import globals from '../globals';
import CONSTANTS from '../../../shared/constants';

global.sinon = sinon.createSandbox();
Enzyme.configure({adapter: new Adapter()});

window.process = window.process || {};
window.process.env = window.process.env || {};
Object.assign(window.process.env, CONSTANTS);

window.Maya = globals;
window.Ya = {
  Rum: {
    init() {},
    initErrors() {},
    sendHeroElement() {},
    logError() {}
  }
};
window.mail = {
  onEvent() {}
};
window.webkit = {
  messageHandlers: {
    ololo: {
      postMessage() {}
    },
    oloLo: {
      postMessage() {}
    }
  }
};

require('entries/utils/moment-config').init(globals.config.date);

setTankerProjectId(CONSTANTS.TANKER_PROJECT_ID);
addTranslation(
  globals.session.locale,
  require(`../../../server/i18n/${globals.session.locale}.js`)
);

afterEach(() => {
  global.sinon.restore();
});
