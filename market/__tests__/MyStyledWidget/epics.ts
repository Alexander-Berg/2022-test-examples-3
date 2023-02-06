import {combineEpics, ofType} from 'redux-observable';
import {switchMap} from 'rxjs/operators';
import {of} from 'rxjs';
import {GenericAction} from '@yandex-market/apiary/common/actions';
import {WidgetEpic} from '@yandex-market/apiary';

import type {Collections, Data} from '.';

const doubleEpic: WidgetEpic<GenericAction, Data, Collections> = actions =>
    actions.pipe(
        ofType('#ONE'),
        switchMap(() => of({type: '#TWO'})),
    );

const inc: WidgetEpic<GenericAction, Data, Collections> = actions =>
    actions.pipe(
        ofType('#TOUCH'),
        switchMap(() => of({type: '#INC', payload: {amount: 1}})),
    );

export default combineEpics(doubleEpic, inc);
