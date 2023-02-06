import { Controller, Get, Post, UseGuards } from '@nestjs/common';

import { SecretkeyGuard } from '../../secretkey.guard';

@Controller('controller')
@UseGuards(SecretkeyGuard)
export class ControllerController {
    @Get('get')
    get() {
        return {};
    }

    @Post('post')
    post() {
        return {};
    }
}
