import {WebSocket} from 'ws';

import {Request, Response} from '@yandex-data-ui/core/lib/types';
import {ISocketRequestParams} from 'server/utilities/TestRequestManager/types/connectParams';

import {IDependencies} from 'server/getContainerConfig';
import {ApiRequestChannel} from 'server/sockets/apiRequestsChannel';

export class TestControlPanelController {
    sessionKey: string;
    apiRequestsChannelServer: ApiRequestChannel;

    constructor({sessionKey, apiRequestsChannel}: IDependencies) {
        this.sessionKey = sessionKey;
        this.apiRequestsChannelServer = apiRequestsChannel;
    }

    apiRequestsChannel(req: Request, res: Response): void {
        const {pageToken}: ISocketRequestParams = req.query;

        res.websocket('apiRequestsChannel', (socket: WebSocket) => {
            this.apiRequestsChannelServer.addClientSocket({
                sessionKey: this.sessionKey,
                socket,
                pageToken: pageToken ?? null,
            });
        });
    }
}
