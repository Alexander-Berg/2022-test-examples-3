import albumsReducer from '../../../../components/redux/store/reducers/albums';
import { UPDATE_ALBUMS, FETCH_ALBUMS_SUCCESS } from '../../../../components/redux/store/actions/albums-types';

describe('albums reducer', () => {
    it('должен вернуть начальное состояние', () => {
        expect(albumsReducer(undefined, { type: 'INITIAL' })).toMatchSnapshot();
    });
    it('должен обновить флаги', () => {
        expect(albumsReducer({
            isLoaded: false,
            isLoading: true,
            albums: {
                camera: {
                    id: 'camera',
                    count: 0,
                    name: i18n('%ufo__photoslice_filter_camera')
                },
                videos: {
                    id: 'videos',
                    count: 0,
                    name: i18n('%ufo__photoslice_filter_videos')
                },
                screenshots: {
                    id: 'screenshots',
                    count: 0,
                    name: i18n('%ufo__photoslice_filter_screenshots')
                },
                beautiful: {
                    id: 'beautiful',
                    count: 0,
                    name: i18n('%ufo__photoslice_filter_beautiful')
                },
                unbeautiful: {
                    id: 'unbeautiful',
                    count: 0,
                    name: i18n('%ufo__photoslice_filter_unbeautiful')
                }
            }
        }, { type: UPDATE_ALBUMS, payload: {
            isLoading: false,
            isLoaded: true
        } })).toMatchSnapshot();
    });
    it('должен обновить список альбомов', () => {
        expect(albumsReducer({
            isLoaded: false,
            isLoading: true,
            albums: {
                camera: {
                    id: 'camera',
                    count: 0,
                    name: i18n('%ufo__photoslice_filter_camera')
                },
                videos: {
                    id: 'videos',
                    count: 0,
                    name: i18n('%ufo__photoslice_filter_videos')
                },
                screenshots: {
                    id: 'screenshots',
                    count: 0,
                    name: i18n('%ufo__photoslice_filter_screenshots')
                },
                beautiful: {
                    id: 'beautiful',
                    count: 0,
                    name: i18n('%ufo__photoslice_filter_beautiful')
                },
                unbeautiful: {
                    id: 'unbeautiful',
                    count: 0,
                    name: i18n('%ufo__photoslice_filter_unbeautiful')
                }
            }
        }, { type: FETCH_ALBUMS_SUCCESS, payload: {
            albums: {
                videos: {
                    count: 1
                }
            }
        } })).toMatchSnapshot();
    });
});
