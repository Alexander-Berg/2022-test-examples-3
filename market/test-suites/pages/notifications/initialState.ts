'use strict';

import updateCreationDate from 'spec/hermione/lib/helpers/updateCreationDate';
import initialState from 'spec/lib/page-mocks/notifications.json';

export default updateCreationDate(initialState, 'notifications', 'creationDate');
