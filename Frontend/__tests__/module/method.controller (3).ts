import { Controller, Get, Req, UseInterceptors } from '@nestjs/common';
import { Request } from 'express';

import { XForwardedForFixInterceptor } from '../../xForwardedForFix.interceptor';

@Controller('method')
export class MethodController {
    @Get('header')
    @UseInterceptors(new XForwardedForFixInterceptor())
    getHeader(@Req() req: Request) {
        return {
            header: req.headers['x-forwarded-for'],
        };
    }

    @Get('header2')
    @UseInterceptors(XForwardedForFixInterceptor)
    getHeader2(@Req() req: Request) {
        return {
            header: req.headers['x-forwarded-for'],
        };
    }
}
