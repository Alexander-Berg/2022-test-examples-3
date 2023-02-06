import {compose, identity} from 'ramda';

import WidgetTypes from 'src/widgets-main/common/constants/widgetTypes';
import {setStateWidgetThemeId} from 'src/widgets-main/common/specHelpers/state';
import {Creator as MultiTypeWidgetCreator} from 'src/widgets-create/client/helpers/widgetsCreator/concrete/multiType';

import {withStubs} from '../../helpers/withStubs';
import {createInitialState} from './state.fixtures';

const processInitialParams = ({params}) => {
    const {themeId, themeRows} = params;

    const processedParams = {
        ...params,
    };

    if (themeId) {
        processedParams.themeId = Number(themeId);
    }

    if (themeRows) {
        processedParams.themeRows = Number(themeRows);
    }

    return processedParams;
};

const dataProvider = params => {
    const {themeRows, warningCode, warningText, themeId, offersCount, modelsCount} = params;

    const data = compose(
        themeId ? setStateWidgetThemeId(themeId) : identity,
        createInitialState({
            themeRows,
            warningCode,
            warningText,
            offersCount,
            modelsCount,
        }),
    )();

    return {
        type: WidgetTypes.Models,
        data,
    };
};

export default withStubs({dataProvider, processInitialParams}, MultiTypeWidgetCreator);
