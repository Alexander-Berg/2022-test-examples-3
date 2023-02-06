import Mock from '../../Mock';
import {Request} from '../../mockExecutor';

type State = {
    bar: number;
    baz: number;
};

export default class TestMock extends Mock {
    async init(): Promise<void> {
        await this.state.set('foo', {
            bar: 123,
            baz: 456,
        });
    }

    async action(req: Request<null, {type: 'bar' | 'baz'}>): Promise<number> {
        return (await this.state.get<State>('foo'))[req.params.type!] ?? null;
    }
}
