import {Widget} from '@yandex-market/apiary';

import view from './view';
import controller from './controller';
import widgetReducer from './reducer';
import widgetEpics from './epics';

export type Item = {
    id: number;
    name: string;
};

export type Options = {
    items: number[];
};

export type Data = {
    items: number[];
    isRobot: boolean;
    state: 'none' | 'loading' | 'done';
    touches: number;
};

export type Collections = {
    list: {[key: string]: Item};
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
