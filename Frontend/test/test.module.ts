import { DynamicModule } from '@nestjs/common';

import { RequestScopeService } from './requestScope.service';
import { SingletonScopeService } from './singletonScope.service';

export type TestModuleOptions = {
    requestScopeKeys?: string[];
    singletonScopeKeys?: string[];
};

export class TestModule {
    static register({
        requestScopeKeys = [],
        singletonScopeKeys = [],
    }: TestModuleOptions): DynamicModule {
        const requestScopeProviders = requestScopeKeys.map((key) => ({
            provide: key,
            useClass: RequestScopeService,
        }));

        const singletonScopeProviders = singletonScopeKeys.map((key) => ({
            provide: key,
            useClass: SingletonScopeService,
        }));

        const providers = [
            ...requestScopeProviders,
            ...singletonScopeProviders,
        ];

        return {
            providers,
            exports: providers,
            module: TestModule,
        };
    }
}
