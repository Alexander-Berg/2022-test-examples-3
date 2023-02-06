import { Controller, Get } from '@nestjs/common';

@Controller('simple')
export class SimpleController {
    @Get()
    get() {
        return {};
    }
}
