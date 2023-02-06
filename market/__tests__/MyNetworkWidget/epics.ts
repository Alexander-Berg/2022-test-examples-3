import {combineEpics, ofType} from 'redux-observable';
import {switchMap} from 'rxjs/operators';
import {of} from 'rxjs';
import {GenericAction} from '@yandex-market/apiary/common/actions';
import {WidgetEpic} from '@yandex-market/apiary';

import type {Data} from '.';

const epic: WidgetEpic<GenericAction, Data, []> = actions =>
    actions.pipe(
        ofType('#ONE'),
        switchMap(() =>
            fetch(
                'http://pers-qa.tst.vs.market.yandex.net/question/category/test/test/test',
            ),
        ),
        switchMap(res => res.text()),
        switchMap(content => of({type: '#TWO', payload: {content}})),
    );

const nonMockedNetworkEpic: WidgetEpic<GenericAction, Data, []> = actions =>
    actions.pipe(
        ofType('#THREE'),
        switchMap(() => fetch('http://kadavr.vs.market.yandex.net/ping')),
        switchMap(res => res.text()),
        switchMap(content => of({type: '#TWO', payload: {content}})),
    );

export default combineEpics(epic, nonMockedNetworkEpic);
