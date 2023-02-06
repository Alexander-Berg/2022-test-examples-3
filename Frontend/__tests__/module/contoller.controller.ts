import { Controller, Get, UseInterceptors } from '@nestjs/common';

import { CspInterceptor } from '../../csp.interceptor';

@Controller('controller')
@UseInterceptors(CspInterceptor)
export class ControllerController {
    @Get()
    get() {
        return {};
    }
}
