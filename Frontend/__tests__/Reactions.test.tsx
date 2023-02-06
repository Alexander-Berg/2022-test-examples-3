/* eslint-disable @typescript-eslint/no-explicit-any */
import Enzyme from 'enzyme';
import Adapter from '@wojtekmaj/enzyme-adapter-react-17';
import { TReactions, TReactionType } from 'mg/components/Reactions/Reactions.types';
import { formatReaction, getCounter } from '../helpers/formatReaction';
import { getTotalCount } from '../helpers/getTotalCount';

Enzyme.configure({ adapter: new Adapter() });

const counts: TReactions<TReactionType, number> = {
  Haha: 123,
  Angry: 3,
  Bored: 100,
  Like: 60,
  Dislike: 12,
  Sad: 73,
  Wow: 911,
};
const availableReactions = Object.keys(counts);

describe('Reactions (mg)', () => {
  describe('Helpers', () => {
    describe('#getTotalCount', () => {
      it('should work with empty counts', () => {
        const result = getTotalCount({
          availableReactions,
          counts: {},
        });

        expect(result).toStrictEqual(0);
      });

      it('should work with one count', () => {
        const result = getTotalCount({
          availableReactions,
          counts: {
            Haha: 123,
          },
        });

        expect(result).toStrictEqual(123);
      });

      it('should work with full counts', () => {
        const result = getTotalCount({ availableReactions, counts });

        expect(result).toStrictEqual(1282);
      });

      it('should work with large numbers', () => {
        const counts = {
          Haha: 1e9,
          Angry: 1e7,
          Bored: 1 << 20,
          Like: 1e8,
          Dislike: 1e9,
          Sad: 10,
          Wow: 1,
        };
        const result = getTotalCount({ availableReactions, counts });

        expect(result).toStrictEqual(2111048587);
      });

      it('should work with outdated counts', () => {
        const result = getTotalCount({
          counts: {},
          availableReactions,
          reaction: 'Haha',
        });

        expect(result).toStrictEqual(1);
      });

      it('should not add number for the same type', () => {
        const result = getTotalCount({
          counts: {
            Haha: 1,
          },
          availableReactions,
          reaction: 'Haha',
        });

        expect(result).toStrictEqual(1);
      });

      it('should take outdated counts into account when adding up', () => {
        const result = getTotalCount({
          counts: {
            Wow: 1,
          },
          availableReactions,
          reaction: 'Haha',
        });

        expect(result).toStrictEqual(2);
      });

      it('should take into account the existing quantity when adding up', () => {
        const result = getTotalCount({
          counts: {
            Haha: 1,
            Angry: 1,
            Bored: 1,
            Like: 1,
            Dislike: 1,
            Sad: 1,
            Wow: 1,
          },
          availableReactions,
          reaction: 'Haha',
        });

        expect(result).toStrictEqual(7);
      });

      it('should ignore user reaction', () => {
        const result = getTotalCount({
          counts: {
            Haha: 1,
            Angry: 1,
            Bored: 1,
            Like: 1,
            Dislike: 1,
            Sad: 1,
            Wow: 1,
          },
          availableReactions,
          reaction: 'Haha',
          ignoreUserReaction: true,
        });

        expect(result).toStrictEqual(6);
      });

      it('should ignore user reaction without counts', () => {
        const result = getTotalCount({
          counts: {},
          availableReactions,
          reaction: 'Haha',
          ignoreUserReaction: true,
        });

        expect(result).toStrictEqual(0);
      });
    });

    describe('#getCounter', () => {
      it('should render nothing', () => {
        const result = getCounter({ total: getTotalCount({ counts: {}, availableReactions }) });

        expect(result).toEqual('');
      });

      it('should compute and render 1', () => {
        const result = getCounter({ total: getTotalCount({ counts, availableReactions }) });

        expect(result).toEqual('1.2K');
      });

      it('should compute and render 2', () => {
        const result = getCounter({ total: getTotalCount({
          counts,
          availableReactions,
          reaction: 'Haha',
        }) });

        expect(result).toEqual('1.2K');
      });

      it('should compute and render 3', () => {
        const result = getCounter({ total: getTotalCount({
          counts: {},
          availableReactions,
          reaction: 'Haha',
          ignoreUserReaction: true,
        }) });

        expect(result).toEqual('');
      });
    });

    describe('#formatReaction', () => {
      it('should render nothing', () => {
        const result = formatReaction({ counts: {}, availableReactions });

        expect(result).toMatchObject({
          label: 'Оценить',
          counter: '',
        });
      });

      it('should render count', () => {
        const result = formatReaction({ counts, availableReactions });

        expect(result).toMatchObject({
          label: 'Оценить',
          counter: '1.2K',
        });
      });

      it('should render message with count', () => {
        const result = formatReaction({ counts, availableReactions, reaction: 'Haha' });

        expect(result).toMatchObject({
          label: 'Вы',
          counter: '+1.2K',
        });
      });

      it('should render message', () => {
        const result = formatReaction({ counts: {}, availableReactions, reaction: 'Haha' });

        expect(result).toMatchObject({
          label: 'Вы оценили',
          counter: '',
        });
      });

      it('should work with huge numbers', () => {
        const counts = {
          Haha: 1e9,
          Angry: 1e7,
          Bored: 1 << 20,
          Like: 1e8,
          Dislike: 1e9,
          Sad: 10,
          Wow: 1,
        };

        const result = formatReaction({ counts, availableReactions, reaction: 'Haha' });

        expect(result).toMatchObject({
          label: 'Вы',
          counter: '+2,111M',
        });
      });
    });
  });
});
