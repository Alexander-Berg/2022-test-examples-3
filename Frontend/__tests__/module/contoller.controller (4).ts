import { Controller, Get, UseInterceptors } from '@nestjs/common';

import { YandexuidInterceptor } from '../../yandexuid.interceptor';

@Controller('controller')
@UseInterceptors(YandexuidInterceptor)
export class ControllerController {
    @Get()
    getHeader() {
        return {};
    }
}
