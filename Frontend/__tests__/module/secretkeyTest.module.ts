import { Module } from '@nestjs/common';

import { SecretkeyModule } from '../../secretkey.module';

import { ControllerController } from './contoller.controller';
import { SimpleController } from './simple.controller';
import { MethodController } from './method.controller';

@Module({
    imports: [SecretkeyModule],
    controllers: [ControllerController, MethodController, SimpleController],
})
export class SecretkeyTestModule {}
