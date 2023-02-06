import Bluebird from 'bluebird';
import { LoadState } from 'types/LoadState';
import { Loader } from './Loader';
import { LoadMetaImpl } from '../LoadMeta';

describe('Loader', () => {
  it('loads with success', async () => {
    const meta = new LoadMetaImpl();
    const handleLoad = () => Bluebird.resolve(1);
    const loader = new Loader(meta, handleLoad);

    await loader.load();

    expect(meta.data).toBe(1);
    expect(meta.error).toBe(undefined);
    expect(meta.state).toBe(LoadState.Success);
  });

  it('loads with error', async () => {
    const meta = new LoadMetaImpl();
    const handleLoad = () => Bluebird.reject(new Error('error'));
    const loader = new Loader(meta, handleLoad);

    try {
      await loader.load();
    } catch (error) {
      expect(meta.data).toBe(undefined);
      expect(meta.error!.message).toBe('error');
      expect(meta.state).toBe(LoadState.Fail);
    }
  });

  it('cancels request', async () => {
    const meta = new LoadMetaImpl();
    const handleLoad = () => Bluebird.resolve(1);
    const loader = new Loader(meta, handleLoad);

    loader.load();
    loader.destroy();

    expect(meta.data).toBe(undefined);
    expect(meta.error).toBe(undefined);
    expect(meta.state).toBe(LoadState.Pending);
  });
});
