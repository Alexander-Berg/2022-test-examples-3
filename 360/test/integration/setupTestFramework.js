import 'cross-fetch/polyfill';
import 'jest-dom/extend-expect';
import 'react-testing-library/cleanup-after-each';
import {setTankerProjectId, addTranslation} from 'react-tanker';
import nock from 'nock';

import globals from '../globals';
import CONSTANTS from '../../../shared/constants';

global.document.createRange = () => ({
  setStart: () => {},
  setEnd: () => {},
  commonAncestorContainer: {
    nodeName: 'BODY',
    ownerDocument: document
  }
});

window.Maya = globals;
window.Ya = {
  Rum: {
    init() {},
    initErrors() {},
    sendHeroElement() {},
    logError() {}
  }
};

nock.disableNetConnect();

setTankerProjectId(CONSTANTS.TANKER_PROJECT_ID);
addTranslation(
  globals.session.locale,
  require(`../../../server/i18n/${globals.session.locale}.js`)
);

require('entries/utils/moment-config').init(globals.config.date);

jest.setTimeout(30000);
