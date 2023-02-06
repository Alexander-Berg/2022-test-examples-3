import {createAction} from 'typesafe-actions';

import {IApiRequestInfo} from 'server/utilities/TestRequestManager/types/requestInfo';

export const setApiRequestInfoItems = createAction(
    'test/setApiRequestInfoItems',
)<IApiRequestInfo[]>();
