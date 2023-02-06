import {noop} from 'src/main/common/helpers/toolkit';

const apiProviderStub = () => ({
    metrikaReachGoal: noop,
    partnerMetrikaReachGoal: noop,
});

const processInitialParamsStub = ({params}) => params;

export const withStubs = (
    {dataProvider, apiProvider = apiProviderStub, processInitialParams = processInitialParamsStub},
    WidgetCreator,
) => {
    return class extends WidgetCreator {
        _getDataProviderParams() {
            return processInitialParams(this._createParams);
        }

        _getDataProvider() {
            return dataProvider;
        }

        _getApiProvider() {
            return apiProvider;
        }
    };
};
