import { EntityPermissionType } from 'common/types/entity';
import { IProblem } from 'common/types/problem';

import { IProblemSettingsRequest } from 'client/store/problem-settings/types';

export interface OwnProps {
    problemId: string;
}

export interface StateProps {
    fetchStarted: boolean;
    settings: IProblem;
    permission: EntityPermissionType;
}

export interface DispatchProps {
    getSettings: (_params: IProblemSettingsRequest) => void;
}

export type Props = OwnProps & StateProps & DispatchProps;
