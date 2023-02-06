import { Controller, Get } from '@nestjs/common';
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
