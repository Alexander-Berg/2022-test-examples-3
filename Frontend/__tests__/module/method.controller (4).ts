import { Controller, Get, UseInterceptors } from '@nestjs/common';

import { YandexuidInterceptor } from '../../yandexuid.interceptor';

@Controller('method')
export class MethodController {
    @Get()
    @UseInterceptors(YandexuidInterceptor)
    getHeader() {
        return {};
    }
}
