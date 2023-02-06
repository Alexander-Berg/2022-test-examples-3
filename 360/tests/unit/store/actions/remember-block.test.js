import '../../noscript';
import resourceHelper from '../../../../components/helpers/resource';
import createStore from '../../../../components/redux/store/create-store';
import { updateBlockAndResources } from '../../../../components/redux/store/actions/remember-block';
import { isExistsResource } from '../../../../components/redux/store/selectors/resource';

describe('remember block actions', () => {
    describe('updateBlockAndResources', () => {
        let store;
        beforeEach(() => {
            store = createStore({
                user: { sids: [] },
                environment: { agent: { isMobile: true }, session: { experiment: {} } },
                defaultFolders: {
                    folders: {}
                },
                settings: {},
                config: {}
            });
        });

        it('Должен корректно обрабатывать удалённый блок', () => {
            store.dispatch(updateBlockAndResources({ isRemoved: true }));
            expect(store.getState().rememberBlock).toEqual({ isRemoved: true, isLoading: false });
        });

        it('Должен создавать блок, ресурсы для блока и модели ресурсов', () => {
            const rawResources = [{
                id: '/disk/photo1.jpg'
            }, {
                id: '/disk/photo2.jpg'
            }];

            store.dispatch(updateBlockAndResources({
                id: 'e6e37424-ba6c-48e6-bda8-e0347c47d6e6',
                title1: 'Ваши фотографии за неделю',
                title2: 'Январь 2019',
                resources: rawResources
            }));

            const { resources, rememberBlock } = store.getState();

            expect(rememberBlock).toEqual({
                id: 'e6e37424-ba6c-48e6-bda8-e0347c47d6e6',
                isLoading: false,
                title1: 'Ваши фотографии за неделю',
                title2: 'Январь 2019'
            });

            const resource1 = resourceHelper.preprocess(rawResources[0]);
            resourceHelper.preprocess(resource1);
            expect(resources['/disk/photo1.jpg']).toEqual(resource1);

            const resource2 = resourceHelper.preprocess(rawResources[1]);
            resourceHelper.preprocess(resource2);
            expect(resources['/disk/photo2.jpg']).toEqual(resource2);

            expect(resources['/remember/e6e37424-ba6c-48e6-bda8-e0347c47d6e6']).toEqual({
                id: '/remember/e6e37424-ba6c-48e6-bda8-e0347c47d6e6',
                isComplete: true,
                children: ['/disk/photo1.jpg', '/disk/photo2.jpg']
            });

            expect(isExistsResource(store.getState(), '/disk/photo1.jpg')).toBeTruthy();
            expect(isExistsResource(store.getState(), '/disk/photo2.jpg')).toBeTruthy();
        });
    });
});
