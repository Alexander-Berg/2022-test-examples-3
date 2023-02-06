import withEntityName from 'client/decorators/with-entity-name';
import Tests from 'client/pages/problems/tests/tests';
import { Props } from 'client/pages/problems/tests/types';

export default withEntityName<Props>('problem')(Tests);
