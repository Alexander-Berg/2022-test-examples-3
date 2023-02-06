import {makeShotSuite} from 'spec/utils';

import defaultDetalizationFilter from './suites/defaultDetalizationfilter';
import dayDetalizationChart from './suites/dayDetalizationChart';
import weekDetalizationChart from './suites/weekDetalizationChart';
import commonDataReport from './suites/commonDataReport';
import chartParameters from './suites/chartParameters';

export default makeShotSuite({
    suiteName: 'Costs and Sales',
    childSuites: [
        defaultDetalizationFilter,
        dayDetalizationChart,
        weekDetalizationChart,
        commonDataReport,
        chartParameters,
    ],
});
