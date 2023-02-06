import {TSelector} from 'src/redux/types/TSelector';
import {IApiRequestInfo} from 'server/utilities/TestRequestManager/types/requestInfo';

export const getApiRequestInfoItems: TSelector<IApiRequestInfo[]> = state =>
    state.testControlPanel.apiRequestInfoItems;
