import { Controller, Get, Req, UseInterceptors } from '@nestjs/common';
import { Request } from 'express';

import { XForwardedForFixInterceptor } from '../../xForwardedForFix.interceptor';

@Controller('controller')
@UseInterceptors(new XForwardedForFixInterceptor())
export class ControllerController {
    @Get('header')
    getHeader(@Req() req: Request) {
        return {
            header: req.headers['x-forwarded-for'],
        };
    }
}
