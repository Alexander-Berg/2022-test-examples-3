import {Widget} from '@yandex-market/apiary';

import view from './view';
import controller from './controller';
import widgetReducer from './reducer';
import widgetEpics from './epics';

export type Data = {
    content: string;
};

export default Widget.describe({
    name: '@testament/MyWidget',
    view,
    controller,
    reducers: {
        widget: widgetReducer,
    },
    epics: {
        widget: [widgetEpics],
    },
});
