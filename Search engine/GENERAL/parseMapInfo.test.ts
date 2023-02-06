import {parseMapInfo} from './parseMapInfo';

describe('parseMapInfo', () => {
    test('should correct parse incoming string ', () => {
        expect(parseMapInfo('11.1,22.2')).toEqual({
            screenPosLong: 11.1,
            screenPosLat: 22.2,
        });
        expect(parseMapInfo('11.1,22.2@@z=1')).toEqual({
            screenPosLong: 11.1,
            screenPosLat: 22.2,

            zoom: 1,
        });
        expect(parseMapInfo('11.1,22.2@@33.3,44.4')).toEqual({
            screenPosLong: 11.1,
            screenPosLat: 22.2,
            screenWidth: 33.3,
            screenHeight: 44.4,
        });
        expect(parseMapInfo('11.1,22.2@@33.3,44.4@@55.5,66.6')).toEqual({
            screenPosLong: 11.1,
            screenPosLat: 22.2,
            screenWidth: 33.3,
            screenHeight: 44.4,
            userPosLat: 55.5,
            userPosLong: 66.6,
        });
        expect(parseMapInfo('11.1,22.2@@33.3,44.4@@55.5,66.6@@z=1')).toEqual({
            screenPosLong: 11.1,
            screenPosLat: 22.2,
            screenWidth: 33.3,
            screenHeight: 44.4,
            userPosLat: 55.5,
            userPosLong: 66.6,
            zoom: 1,
        });
    });
});
