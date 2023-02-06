import { AppLayer } from '@dataLayers/app';
import { Partner } from '@dataLayers/partner/partner';
import { TParams, YENV } from '@typings/app';
import { IUser } from '@typings/user';
import { IRoute } from '@typings/router';
import { Feature } from '@dataLayers/feature';
import { HistoryLayer } from '@dataLayers/history';
import { makeObservable, observable } from 'mobx';
import { TFeatureName } from '@typings/feature';
import { createMemoryHistory } from 'history';

export class HistoryLayerMock extends HistoryLayer {
    constructor() {
        super(createMemoryHistory());
    }
}

export class FeatureLayerMock extends Feature {
    constructor(layers: {app: AppLayer, partner: Partner}, enabledFeatures: TFeatureName[] = []) {
        super(layers, {});
        for (const f of enabledFeatures) {
            this.cache.set(f, {
                enabled: true,
            });
        }
    }

    setEnabledFeatures(enabledFeatures: TFeatureName[]) {
        this.cache.clear();
        for (const f of enabledFeatures) {
            this.cache.set(f, {
                enabled: true,
            });
        }
    }
}

export class AppLayerMock implements AppLayer {
    isYandexNet: boolean = true;
    readonly layers: { readonly history: HistoryLayer } = {
        history: new HistoryLayerMock(),
    } as const;
    @observable.ref
    route: IRoute | null = null;
    user: IUser = {
        id: 'someUserId',
        emails: ['someUser@test.ru'],
    };
    @observable.struct
    yenv: YENV = YENV.production;
    @observable.struct
    partnerId: number = 1
    @observable.struct
    currentUrl: string = 'https://'
    @observable.struct
    lastPath: string = ''
    @observable.struct
    params: TParams = {}
    @observable.struct
    searchQuery: string | undefined
    @observable.struct
    selectedPartnerId: null | number = null
    @observable.struct
    sortBy: [string, string] | undefined

    constructor() {
        makeObservable(this);
    }
    applyParams = jest.fn()
    destroy = jest.fn()
    init = jest.fn()
    setRoute = jest.fn()
    isCurrentUser = jest.fn(() => true)
}
