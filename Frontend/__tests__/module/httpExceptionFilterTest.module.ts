import { Controller, ForbiddenException, Get, Module, UseFilters } from '@nestjs/common';
import { Observable } from 'rxjs';

import { HttpExceptionFilter } from '../../httpException.filter';

@Controller()
@UseFilters(HttpExceptionFilter)
class TestController {
    @Get('throw')
    throw() {
        throw new Error('foo');
    }

    @Get('forbidden')
    forbidden() {
        throw new ForbiddenException({ key: 'forbidden', message: 'No access' });
    }

    @Get('observableError')
    observableError() {
        return new Observable((subscriber) => {
            subscriber.error(new Error('foo'));
        });
    }

    @Get('observableException')
    observableException() {
        return new Observable((subscriber) => {
            subscriber.error(new ForbiddenException({ key: 'forbidden', message: 'No access' }));
        });
    }
}

@Module({
    controllers: [TestController],
})
export class HttpExceptionFilterTestModule {}
