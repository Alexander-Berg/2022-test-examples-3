import { shallow } from 'enzyme';
import * as React from 'react';

import { Input } from '../../../../../ui/Input';
import { SEARCH_ID_TYPE } from '../../types';
import { WalletsSearchBar } from '../component';

const onChangeSearch = jest.fn();

describe('WalletsSearchBar', () => {
    it('should initially render correctly', () => {
        const walletSearchBar: any = shallow(
            <WalletsSearchBar searchId={'123345667'}
                              onChangeSearch={onChangeSearch}
                              searchIdType={SEARCH_ID_TYPE.ORGANIZATION_ID}
                              expectedSearchIdType={SEARCH_ID_TYPE.ORGANIZATION_ID}/>,
        );
        expect(walletSearchBar).toMatchSnapshot();
    });

    it('should have different SearchType when typing in different inputs', function () {
        const walletSearchBar: any = shallow(
            <WalletsSearchBar searchId={'123345667'}
                              onChangeSearch={onChangeSearch}
                              searchIdType={SEARCH_ID_TYPE.ORGANIZATION_ID}
                              expectedSearchIdType={SEARCH_ID_TYPE.ORGANIZATION_ID}/>,
        );

        walletSearchBar.find(Input).simulate('change', '2350');
        expect(onChangeSearch).toBeCalledWith(SEARCH_ID_TYPE.ORGANIZATION_ID, '2350');
    });
});
