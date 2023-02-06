import createEpicExecutor from '../../epicExecutor';
import epic from './epics';
import {allItems} from './data';

import {Collections, Data} from '.';

const callEpic = createEpicExecutor<Data, Collections>(
    {
        items: [0, 1],
        isRobot: false,
        state: 'none',
        touches: 0,
    },
    {list: allItems},
)(epic);

describe('confirm', () => {
    it('Должен установить cookie и перезагрузить страницу', async () => {
        const action = await callEpic({type: '#ONE'}).toPromise();
        expect(action).toEqual({type: '#TWO'});
    });
});
