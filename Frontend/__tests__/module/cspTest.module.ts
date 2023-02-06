import { Module } from '@nestjs/common';

import { CspModule } from '../../csp.module';

import { ControllerController } from './contoller.controller';
import { SimpleController } from './simple.controller';
import { MethodController } from './method.controller';
import { ServiceController } from './service.controller';

@Module({
    imports: [CspModule],
    controllers: [ControllerController, MethodController, SimpleController, ServiceController],
})
export class CspTestModule {}
