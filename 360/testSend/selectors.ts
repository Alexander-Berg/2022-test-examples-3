import { IReduxState } from './types';
import { FEATURE_NAME } from './constants';

export const getTestSendState = (state: IReduxState) => state[FEATURE_NAME];

export { getActiveProjectSlugSafe } from '@/features/projects/selectors';
export { getLetterUserTemplatesVariables } from '@/features/campaign/selectors';
