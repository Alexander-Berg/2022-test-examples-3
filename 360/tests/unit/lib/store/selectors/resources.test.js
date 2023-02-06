import { resourceView, isResourcesEqual, resourcesSelectorCreator } from '../../../../../lib/store/selectors/resources';
import { RESOURCE_VIEWS } from '../../../../../lib/consts';

const someResources = [{
    id: 'first',
    meta: {
        size: 2048
    }
}, {
    id: 'second',
    meta: {
        size: 1024
    }
}, {
    id: 'third',
    meta: {
        mimetype: 'image/jpeg'
    }
}];

describe('selectors/resources =>', () => {
    describe('resourceView =>', () => {
        let resource;
        const originalAudio = global.Audio;
        const playableAudioMimes = ['audio/mp3'];
        beforeAll(() => {
            global.Audio = function() {
                this.canPlayType = (mime) => playableAudioMimes.includes(mime);
            };
        });
        afterAll(() => {
            global.Audio = originalAudio;
        });
        beforeEach(() => {
            resource = {
                type: 'file',
                title: 'имя-ресурса',
                meta: {
                    mediatype: 'image',
                    mimetype: 'image/jpeg',
                    hasPreview: true
                }
            };
        });

        it('should return RESOURCE_VIEWS.NONE if no resource given', () => {
            expect(resourceView()).toEqual(RESOURCE_VIEWS.NONE);
        });
        it('should return RESOURCE_VIEWS.NONE if null given', () => {
            expect(resourceView(null)).toEqual(RESOURCE_VIEWS.NONE);
        });
        it('should return RESOURCE_VIEWS.ERROR if errorCode is in resource', () => {
            resource.errorCode = 500;
            expect(resourceView(resource)).toEqual(RESOURCE_VIEWS.ERROR);
        });
        it('should return RESOURCE_VIEWS.ERROR if resource is blocked', () => {
            resource.blocked = true;
            expect(resourceView(resource)).toEqual(RESOURCE_VIEWS.ERROR);
        });
        it('should return RESOURCE_VIEWS.DIR if resource is directory', () => {
            resource.type = 'dir';
            resource.meta = {};
            expect(resourceView(resource)).toEqual(RESOURCE_VIEWS.DIR);
        });
        it('should return RESOURCE_VIEWS.ALBUM if resource is directory', () => {
            resource.type = 'album';
            resource.meta = {};
            expect(resourceView(resource)).toEqual(RESOURCE_VIEWS.ALBUM);
        });
        it('should return RESOURCE_VIEWS.IMAGE if resource is image with preview', () => {
            expect(resourceView(resource)).toEqual(RESOURCE_VIEWS.IMAGE);
        });
        it('should return RESOURCE_VIEWS.ICON if resource is image without preview', () => {
            resource.meta.hasPreview = false;
            expect(resourceView(resource)).toEqual(RESOURCE_VIEWS.ICON);
        });
        it('should return RESOURCE_VIEWS.VIDEO if resource is video and it is avialable for video player', () => {
            resource.meta.mediatype = 'video';
            resource.isAvialableForVideoPlayer = true;
            expect(resourceView(resource)).toEqual(RESOURCE_VIEWS.VIDEO);
        });
        it('should return RESOURCE_VIEWS.PREVIEW if resource is video and it is not avialable for video player', () => {
            resource.meta.mediatype = 'video';
            resource.isAvialableForVideoPlayer = false;
            expect(resourceView(resource)).toEqual(RESOURCE_VIEWS.PREVIEW);
        });
        it('should return RESOURCE_VIEWS.PREVIEW if resource is video but could not fetch streams', () => {
            resource.meta.mediatype = 'video';
            resource.videoStreamsFetchError = true;
            expect(resourceView(resource)).toEqual(RESOURCE_VIEWS.PREVIEW);
        });
        it('should return RESOURCE_VIEWS.AUDIO if resource is audio and browser can play audio mimetype', () => {
            resource.meta.mediatype = 'audio';
            resource.meta.mimetype = playableAudioMimes[0];
            resource.meta.hasPreview = false;
            expect(resourceView(resource)).toEqual(RESOURCE_VIEWS.AUDIO);
        });
        it('should return RESOURCE_VIEWS.ICON if resource is audio and browser can\'t play audio mimetype', () => {
            resource.meta.mediatype = 'audio';
            resource.meta.mimetype = 'audio/wav';
            resource.meta.hasPreview = false;
            expect(resourceView(resource)).toEqual(RESOURCE_VIEWS.ICON);
        });
        it('should return RESOURCE_VIEWS.ICON if resource is audio and we check it in server', () => {
            resource.meta.mediatype = 'audio';
            resource.meta.mimetype = playableAudioMimes[0];
            resource.meta.hasPreview = false;
            global.Audio = undefined;
            expect(resourceView(resource)).toEqual(RESOURCE_VIEWS.ICON);
        });
        it('should return RESOURCE_VIEWS.PREVIEW if resource has preview and it is not an image', () => {
            resource.meta.mediatype = 'document';
            expect(resourceView(resource)).toEqual(RESOURCE_VIEWS.PREVIEW);
        });
        it('should return RESOURCE_VIEWS.ICON if resource has not preview', () => {
            resource.meta.mediatype = 'document';
            resource.meta.hasPreview = false;
            expect(resourceView(resource)).toEqual(RESOURCE_VIEWS.ICON);
        });
    });

    describe('isResourcesEqual =>', () => {
        it('should return true if resources arrays are equal', () => {
            expect(isResourcesEqual(someResources, someResources)).toEqual(true);
        });
        it('should return true if array are different, but all resource objects are equal', () => {
            const resourcesClone = someResources.concat();
            expect(someResources).not.toBe(resourcesClone);
            expect(isResourcesEqual(someResources, resourcesClone)).toEqual(true);
        });
        it('should return false if resources count is different', () => {
            expect(isResourcesEqual(someResources, someResources.concat({ id: 'fourth' }))).toEqual(false);
        });
        it('should return false if one of resources changed', () => {
            const resourcesClone = someResources.concat();
            resourcesClone[1] = Object.assign({}, resourcesClone[1]);
            expect(isResourcesEqual(someResources, resourcesClone)).toEqual(false);
        });
        it('should return false if resources are the same but in different order', () => {
            const resourcesClone = [someResources[0], someResources[2], someResources[1]];
            expect(isResourcesEqual(someResources, resourcesClone)).toEqual(false);
        });
    });

    describe('resourcesSelectorCreator =>', () => {
        const resourcesSelector = (state) => state.resources[state.current].children.map((resourceId) => state.resources[resourceId]);
        const aKindOfResourceChangeReducer = (state, resourceId, resourcePayload) => {
            return Object.assign({}, state, {
                resources: Object.assign({}, state.resources, {
                    [resourceId]: Object.assign(
                        {},
                        state.resources[resourceId],
                        resourcePayload,
                        {
                            meta: Object.assign(
                                {},
                                state.resources[resourceId].meta,
                                resourcePayload.meta)
                        }
                    )
                })
            });
        };
        let aKindOfState;
        const getResourcesSelector = resourcesSelectorCreator(resourcesSelector, (resources) => resources);
        const rootResourceId = 'root-resource-id';
        beforeEach(() => {
            const resourcesHashMap = someResources.reduce((accu, resource) => {
                accu[resource.id] = resource;
                return accu;
            }, {});
            aKindOfState = {
                current: rootResourceId,
                resources: Object.assign({
                    [rootResourceId]: {
                        id: rootResourceId,
                        children: someResources.map((resource) => resource.id)
                    }
                }, resourcesHashMap)
            };
        });

        it('should return resources array', () => {
            expect(getResourcesSelector(aKindOfState)).not.toBe(someResources);
            expect(getResourcesSelector(aKindOfState)).toEqual(someResources);
        });
        it('should return cached array if child resources not changed', () => {
            const resources = getResourcesSelector(aKindOfState);
            const newState = aKindOfResourceChangeReducer(aKindOfState, rootResourceId, { loading: true });
            expect(newState).not.toBe(aKindOfState);
            expect(newState.resources).not.toBe(aKindOfState.resources);
            expect(newState.resources[rootResourceId]).not.toBe(aKindOfState.resources[rootResourceId]);
            const newResources = getResourcesSelector(newState);
            expect(resources).toBe(newResources); // получили ссылку на тот же (закешированный) массив
        });
        it('should return new array if any of child resources changed', () => {
            const resources = getResourcesSelector(aKindOfState);
            const changingResourceId = someResources[0].id;
            const newState = aKindOfResourceChangeReducer(aKindOfState, changingResourceId, { public: true });
            expect(newState).not.toBe(aKindOfState);
            expect(newState.resources).not.toBe(aKindOfState.resources);
            expect(newState.resources[changingResourceId]).not.toBe(aKindOfState.resources[changingResourceId]);
            const newResources = getResourcesSelector(newState);
            expect(resources).not.toBe(newResources); // получили ссылку на новый массив
            expect(resources[0]).not.toBe(newResources[0]); // ссылка на объект изменённого ресурса поменялась
            expect(resources[0].meta).not.toBe(newResources[0].meta); // ссылка на подобъект meta изменённого ресурса поменялась
            expect(resources[1]).toBe(newResources[1]); // ссылки на другие ресурсы не поменялись
            expect(resources[2]).toBe(newResources[2]);
        });
        it('should return new array if meta of any child resource changed', () => {
            const resources = getResourcesSelector(aKindOfState);
            const changingResourceId = someResources[1].id;
            const newState = aKindOfResourceChangeReducer(aKindOfState, changingResourceId, { meta: { size: 0 } });
            expect(newState).not.toBe(aKindOfState);
            expect(newState.resources).not.toBe(aKindOfState.resources);
            expect(newState.resources[changingResourceId]).not.toBe(aKindOfState.resources[changingResourceId]);
            const newResources = getResourcesSelector(newState);
            expect(resources).not.toBe(newResources); // получили ссылку на новый массив
            expect(resources[0]).toBe(newResources[0]); // ссылки на другие ресурсы не поменялись
            expect(resources[1]).not.toBe(newResources[1]); // ссылка на объект изменённого ресурса поменялась
            expect(resources[1].meta).not.toBe(newResources[1].meta); // ссылка на подобъект meta изменённого ресурса поменялась
            expect(resources[2]).toBe(newResources[2]);
        });
    });
});
