import {Widget} from '@yandex-market/apiary';

import view from '../MyWidget/view';
import controller from '../MyWidget/controller';
import widgetReducer from '../MyWidget/reducer';
import widgetEpics from './epics';

export default Widget.describe({
    name: '@testament/MyWidget2',
    view,
    controller,
    reducers: {
        widget: widgetReducer,
    },
    epics: {
        widget: [widgetEpics],
    },
});
