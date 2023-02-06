import {compose, identity} from 'ramda';

import WidgetTypes from 'src/widgets-main/common/constants/widgetTypes';
import {setStateWidgetThemeId} from 'src/widgets-main/common/specHelpers/state';
import {Creator as MultiTypeWidgetCreator} from 'src/widgets-create/client/helpers/widgetsCreator/concrete/multiType';

import {withStubs} from '../../helpers/withStubs';
import {createInitialState} from './state.fixtures';

const processInitialParams = ({params}) => {
    const {themeId} = params;

    const processedParams = {
        ...params,
    };

    if (themeId) {
        processedParams.themeId = Number(themeId);
    }

    return processedParams;
};

const dataProvider = params => {
    const {warningCode, warningText, outOfStock, themeId} = params;

    const data = compose(
        themeId ? setStateWidgetThemeId(themeId) : identity,
        createInitialState({warningCode, warningText, outOfStock}),
    )();

    return {
        type: WidgetTypes.Offers,
        data,
    };
};

export default withStubs({dataProvider, processInitialParams}, MultiTypeWidgetCreator);
