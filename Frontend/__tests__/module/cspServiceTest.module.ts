import { Module, Controller, Get } from '@nestjs/common';

import { CspModule } from '../../csp.module';
import { CspService } from '../../csp.service';

@Controller('service')
export class ServiceController {
    constructor(private cspService: CspService) {}

    @Get()
    async get() {
        const nonce = await this.cspService.getNonce();

        return {
            nonce,
        };
    }
}

@Module({
    imports: [CspModule],
    controllers: [ServiceController],
})
export class CspServiceTestModule {}
