import { Controller, Get, Post, UseGuards } from '@nestjs/common';

import { SecretkeyGuard } from '../../secretkey.guard';

@Controller('method')
export class MethodController {
    @Get('get')
    @UseGuards(SecretkeyGuard)
    get() {
        return {};
    }

    @Post('post')
    @UseGuards(SecretkeyGuard)
    post() {
        return {};
    }
}
