import {ofType} from 'redux-observable';
import {map, scan} from 'rxjs/operators';

import {Epic} from 'shared/types/redux';
import {AnyAppState} from 'shared/types/reducers';

import {actions, FIBONACCI_LOAD_NEXT} from './actions';

export const loadNextFibonacciNumber: Epic<AnyAppState, any> = actions$ => {
    return actions$.pipe(
        ofType(FIBONACCI_LOAD_NEXT),
        scan(
            ([a, b]) => {
                return [b, a + b];
            },
            [0, 1],
        ),
        map(([n]) => n),
        map(actions.loadNextSuccess),
    );
};
