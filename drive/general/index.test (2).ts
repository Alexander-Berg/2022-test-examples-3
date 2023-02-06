import { IStore } from '../../components/App/store';
import { AdminUserReducerState } from '../../reducers/adminUserReducer';
import { isObjectEqual } from '../isObjectEqual';
import { getRawSetting } from './index';

const validJSON = '{ "title":"Damages Car Model", "type":"object", "areas":[ { "name":"exterior", "display":"Кузов" }, { "name":"interior", "display":"Салон" } ] }';
const invalidJSON = 'not a JSON';

const correctStore: IStore = {
    AdminUser: {
        rawSettings: [{ setting_key: 'key', setting_value: validJSON }],
    } as AdminUserReducerState,
};

const incorrectStore: IStore = {
    AdminUser: {
        rawSettings: [{ setting_key: 'key', setting_value: invalidJSON }],
    } as AdminUserReducerState,
};

describe('Get raw setting', () => {
    it('should work with empty store', () => {
        expect(getRawSetting({}, 'key')).toBe(undefined);
    });

    it('should work without key', () => {
        expect(getRawSetting({ key: 5 } as IStore, '')).toBe(undefined);
    });

    it('should return string when JSON is not valid', () => {
        expect(getRawSetting(incorrectStore, 'key')).toBe(invalidJSON);
    });

    it('should correct work', () => {
        expect(isObjectEqual(getRawSetting(correctStore, 'key'), JSON.parse(validJSON))).toBeTruthy();
    });
});
