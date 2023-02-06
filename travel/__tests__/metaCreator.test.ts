import IState from '../../../../interfaces/state/IState';
import Tld from '../../../../interfaces/Tld';
import IStateFlags from '../../../../interfaces/state/flags/IStateFlags';
import IStateSearchForm from '../../../../interfaces/state/IStateSearchForm';
import IStateCurrencies from '../../../../interfaces/state/IStateCurrencies';
import IStateSearch from '../../../../interfaces/state/search/IStateSearch';
import IStateEnvironment from '../../../../interfaces/state/IStateEnvironment';
import EnvironmentType from '../../../../interfaces/EnvironmentType';

import metaCreator from '../metaCreator';

describe('search metaCreator', () => {
    it('should return empty object if state was not passed', () => {
        const result = metaCreator();

        expect(result).toEqual({});
    });

    it('should return object with data if state was passed', () => {
        const payload = {};
        const state = {
            search: {
                context: {},
            } as IStateSearch,
            currencies: {} as IStateCurrencies,
            otherOptions: {},
            environment: {
                type: EnvironmentType.server,
                production: false,
            } as IStateEnvironment,
            flags: {} as IStateFlags,
            tld: Tld.ru,
            searchForm: {} as IStateSearchForm,
            isTouch: false,
        } as unknown as IState;
        const result = metaCreator(payload, state);

        expect(result).toEqual({
            context: {},
            currencies: {},
            environment: EnvironmentType.server,
            flags: {},
            isProduction: false,
            isTouch: false,
            searchForm: {},
            tld: Tld.ru,
        });
    });
});
