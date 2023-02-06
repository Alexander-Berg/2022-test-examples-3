import { Module, Controller, Get, UseInterceptors } from '@nestjs/common';

import { CspModule } from '../../csp.module';
import { CspInterceptor } from '../../csp.interceptor';

@Controller('controller')
@UseInterceptors(CspInterceptor)
export class ControllerController {
    @Get()
    get() {
        return {};
    }
}

@Controller('method')
export class MethodController {
    @Get()
    @UseInterceptors(CspInterceptor)
    get() {
        return {};
    }
}

@Controller('simple')
export class SimpleController {
    @Get()
    get() {
        return {};
    }
}

@Module({
    imports: [CspModule],
    controllers: [ControllerController, MethodController, SimpleController],
})
export class CspInterceptorTestModule {}
