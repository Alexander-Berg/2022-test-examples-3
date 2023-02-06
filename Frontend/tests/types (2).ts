import { RouteComponentProps } from 'react-router-dom';

import { AddedProps as WithEntityNameProps } from 'client/decorators/with-entity-name/types';

interface IProblemTestsRouteParams {
    id: string;
}

export type Props = RouteComponentProps<IProblemTestsRouteParams> & WithEntityNameProps;
