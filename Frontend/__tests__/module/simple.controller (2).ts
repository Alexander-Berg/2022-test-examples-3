import { Controller, Get, Post } from '@nestjs/common';

@Controller('simple')
export class SimpleController {
    @Get('get')
    get() {
        return {};
    }

    @Post('post')
    post() {
        return {};
    }
}
