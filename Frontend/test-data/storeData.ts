import { Store } from '../store/index';
import { TA_ROUTER_STORE_PATH } from '../routes/const';

import {
    initialStateLoading,
    initialStateError,
    initialStateCommonData,
} from '~/src/common/redux/common.reducers';
import { initialState as initialStateAccountsSpaces } from '../Dispenser.features/Folders/redux/AccountsSpaces.reducer';
import { initialState as initialStateCampaignSettings } from '../redux/reducers/campaignSettings';
import { initialState as initialStateChangeHistory } from '../Dispenser.features/Folders/components/ChangeHistory/ChangeHistory.redux';
import { initialState as initialStateFolders } from '../Dispenser.features/Folders/redux/Folders.reducer';
import { initialState as initialStateTransferForm } from '../Dispenser.features/TransferForm/TransferForm.redux/reducer';
import {
    initialProvidersState,
    initialProviderReserveFoldersState,
    initialProvidersWithSettingsState,
} from '../Dispenser.features/Folders/redux/Providers.reducer';
import { initialState as initialStateQuota } from '../Dispenser.features/Quota/redux/Quota.reducer';
import { initialState as initialStateQuotas } from '../Dispenser.features/Quotas/redux/Quotas.reducer';
import { initialState as initialStateServices } from '../redux/reducers/services';
import { initialState as initialStateSuggestFolders } from '../Dispenser.features/TransferForm/TransferForm.components.legacy/SuggestFolders/SuggestFolders.redux/reducer';
import { initialState as initialStateSummary } from '../Dispenser.features/Summary/redux/Summary.reducer';
import { initialState as initialStateTransfers } from '../Dispenser.features/Folders/redux/Transfers.reducer';
import { initialState as initialStateUsers } from '../Dispenser.features/Folders/redux/Users.reducer';

export const mockStoreData: Store = {
    [TA_ROUTER_STORE_PATH]: '',
    loadingReducer: { ...initialStateLoading },
    errorReducer: { ...initialStateError },
    common: { ...initialStateCommonData },

    accountsSpaces: { ...initialStateAccountsSpaces },
    campaignSettings: { ...initialStateCampaignSettings },
    folders: { ...initialStateFolders },
    foldersChangeHistory: { ...initialStateChangeHistory },
    transferForm: { ...initialStateTransferForm },
    list: { ...initialStateQuotas },
    providers: { ...initialProvidersState },
    providersWithSettings: { ...initialProvidersWithSettingsState },
    providerReserveFolder: { ...initialProviderReserveFoldersState },
    quotas: { ...initialStateQuota },
    services: { ...initialStateServices },
    suggestFolders: { ...initialStateSuggestFolders },
    summaries: { ...initialStateSummary },
    transfers: { ...initialStateTransfers },
    users: { ...initialStateUsers },
};
