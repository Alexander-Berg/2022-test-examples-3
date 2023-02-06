import { Controller, Get, UseInterceptors } from '@nestjs/common';

import { CspInterceptor } from '../../csp.interceptor';

@Controller('method')
export class MethodController {
    @Get()
    @UseInterceptors(CspInterceptor)
    get() {
        return {};
    }
}
