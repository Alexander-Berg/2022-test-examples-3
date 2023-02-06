import { DynamicModule, Inject, Injectable } from "@nestjs/common";

const TEST_CONFIG_VALUE_TOKEN = 'TestConfigValueToken';

@Injectable()
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export class TestConfigService<T = any> {
    config: T;

    constructor(@Inject(TEST_CONFIG_VALUE_TOKEN) configValue: T) {
        this.config = configValue;
    }
}

export class TestConfigModule {
    static register<T>(config: T): DynamicModule {
        return {
            module: TestConfigModule,
            providers: [
                {
                    provide: TEST_CONFIG_VALUE_TOKEN,
                    useValue: config,
                },
                TestConfigService,
            ],
            exports: [TestConfigService],
        }
    }
}
