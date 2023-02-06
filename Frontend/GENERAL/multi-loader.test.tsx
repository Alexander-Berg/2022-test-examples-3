import * as React from 'react';
import { shallow } from 'enzyme';

import { ApiBaseResponse } from 'types/api';

import { MultiLoader } from './multi-loader';

describe('MultiLoader', () => {
    it('не должен вызывать методы API на сервере', () => {
        expect(shallow(<MultiLoader<[ApiBaseResponse, ApiBaseResponse]>
            apiFunctions={[
                () => Promise.resolve({ status: 'ok', value: 'a' }),
                () => { throw 'API functions should not be called on server' },
            ]}
            renderLoading={() => 'must be rendered on server'}
            render={() => 'must be rendered in browser'}
            renderError={() => 'must be rendered in browser'}
        />)).toMatchSnapshot();
    });
});
