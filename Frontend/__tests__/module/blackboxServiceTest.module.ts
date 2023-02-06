import { Controller, Get, Module } from '@nestjs/common';

import { BlackboxService } from '../../blackbox.service';

@Controller('blackbox')
class TestController {
    constructor(private blackboxService: BlackboxService) {}

    @Get()
    async get() {
        const blackbox = await this.blackboxService.getBlackbox();

        return {
            blackbox,
        };
    }
}

@Module({
    controllers: [TestController],
})
export class BlackboxServiceTestModule {}
