import { Module } from '@nestjs/common';

import { ControllerController } from './contoller.controller';
import { SimpleController } from './simple.controller';
import { MethodController } from './method.controller';

@Module({
    controllers: [ControllerController, MethodController, SimpleController],
})
export class XForwardedForFixTestModule {}
