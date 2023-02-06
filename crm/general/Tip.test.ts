import { waitFor } from '@testing-library/react';
import { Tip } from './Tip';
import { Store } from '../Store';
import { Rating } from '../../../types';

describe('Tip', () => {
  let tip: Tip;
  beforeEach(() => {
    tip = new Tip();
    const _store = new Store({ tip });
  });

  describe('.setHtml', () => {
    describe('when argument is defined', () => {
      it('sets .html #1', () => {
        tip.setHtml('test');

        expect(tip.html).toBe('test');
      });

      it('sets .html #2', () => {
        tip.setHtml('');

        expect(tip.html).toBe('');
      });
    });

    describe('when argument is undefined', () => {
      it('sets .html', () => {
        tip.setHtml(undefined);

        expect(tip.html).toBeUndefined();
      });
    });
  });

  describe('.load', () => {
    it('sets .loading.state to true initialy', () => {
      tip.load(1);

      expect(tip.loading.state).toBeTruthy();
    });

    describe('when external handler returns html', () => {
      describe('with length === 0', () => {
        it('sets .html to undefined', () => {
          tip.setExternalHandler(() => Promise.resolve(''));

          tip.load(1);

          expect(tip.html).toBeUndefined();
        });
      });

      describe('with length > 0', () => {
        it('sets .html to it', async () => {
          tip.setExternalHandler(() => Promise.resolve('123'));

          tip.load(1);

          await waitFor(() => {
            expect(tip.html).toBe('123');
          });
        });
      });
    });

    describe('when external handler throws error', () => {
      it('sets .html to undefined', () => {
        tip.setExternalHandler(() => Promise.reject());

        tip.load(1);

        expect(tip.html).toBeUndefined();
      });
    });

    it('sets .loading.state to false finally', async () => {
      tip.loading.on();
      tip.setExternalHandler(() => Promise.resolve('123'));

      tip.load(1);

      await waitFor(() => {
        expect(tip.loading.state).toBeFalsy();
      });
    });
  });

  describe('.reset', () => {
    it('resets .html', () => {
      tip.setHtml('test');

      tip.reset();

      expect(tip.html).toBeUndefined();
    });

    it('calls .loading.on', () => {
      tip.loading.off();

      tip.reset();

      expect(tip.loading.state).toBeTruthy();
    });

    it('calls rating.reset', () => {
      const mockRating = ({ reset: jest.fn() } as unknown) as Rating;
      const tip = new Tip(mockRating);

      tip.reset();

      expect(mockRating.reset).toBeCalledTimes(1);
    });
  });
});
