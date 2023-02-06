import makeProxyInvoker from 'server/utilities/makeProxyInvoker';

import {TestControlPanelController} from './TestControlPanelController';

const testControlPanelController = makeProxyInvoker(TestControlPanelController);

export default {
    apiRequestsChannel: testControlPanelController.apiRequestsChannel,
};
