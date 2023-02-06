import { DynamicModule } from '@nestjs/common';

import { TestService } from './test.service';

export type TestModuleOptions = {
    keys?: string[];
};

export class TestModule {
    static register({ keys = [] }: TestModuleOptions): DynamicModule {
        const providers = keys.map((key) => ({
            provide: key,
            useClass: TestService,
        }));

        return {
            providers,
            exports: providers,
            module: TestModule,
        };
    }
}
