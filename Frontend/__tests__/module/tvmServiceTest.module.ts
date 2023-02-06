import { Controller, Get, Module } from '@nestjs/common';

import { TvmService } from '../../tvm.service';

@Controller('tvm')
class TestController {
    constructor(private tvmService: TvmService) {}

    @Get()
    async get() {
        const tvm = await this.tvmService.getTvm();

        return {
            tvm,
        };
    }

    @Get('getTicket')
    async getTicket() {
        const ticket = await this.tvmService.getTicket('blackbox');

        return {
            ticket,
        };
    }
}

@Module({
    controllers: [TestController],
})
export class TvmServiceTestModule {}
