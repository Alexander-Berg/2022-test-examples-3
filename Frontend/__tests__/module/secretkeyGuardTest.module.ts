import { Module, Controller, Get, Post, UseGuards } from '@nestjs/common';

import { SecretkeyModule } from '../../secretkey.module';
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

@Module({
    imports: [SecretkeyModule],
    controllers: [ControllerController, MethodController, SimpleController],
})
export class SecretkeyGuardTestModule {}
