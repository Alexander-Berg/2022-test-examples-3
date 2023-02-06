import { Controller, Get, Req } from '@nestjs/common';
import { Request } from 'express';

@Controller('simple')
export class SimpleController {
    @Get('header')
    getHeader(@Req() req: Request) {
        return {
            header: req.headers['x-forwarded-for'],
        };
    }
}
