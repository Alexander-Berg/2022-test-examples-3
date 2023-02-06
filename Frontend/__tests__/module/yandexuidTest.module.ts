import { Module } from '@nestjs/common';

import { YandexuidModule } from '../../yandexuid.module';

import { ControllerController } from './contoller.controller';
import { SimpleController } from './simple.controller';
import { MethodController } from './method.controller';

@Module({
    imports: [YandexuidModule],
    controllers: [ControllerController, MethodController, SimpleController],
})
export class YandexuidTestModule {}
