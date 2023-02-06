import { Rating } from './Rating';

describe('Rating', () => {
  describe('.like', () => {
    describe('when was clear', () => {
      it('adds id to .byId', () => {
        const rating = new Rating();

        rating.like(123);

        expect(rating.byId[123]).toBeTruthy();
      });
    });

    describe('when was liked previously', () => {
      let rating: Rating;
      const testId = 123;
      beforeEach(() => {
        rating = new Rating();

        rating.like(testId);
      });

      it('removes id from .byId', () => {
        rating.like(testId);

        expect(rating.byId[testId]).toBe(undefined);
      });
    });

    describe('when was disliked previously', () => {
      let rating: Rating;
      const testId = 123;
      beforeEach(() => {
        rating = new Rating();

        rating.dislike(testId);
      });

      it('overrides id in .byId', () => {
        rating.like(testId);

        expect(rating.byId[testId]).toBe(true);
      });
    });
  });

  describe('.dislike', () => {
    describe('when was clear', () => {
      it('adds id to .byId', () => {
        const rating = new Rating();

        rating.dislike(123);

        expect(rating.byId[123]).toBe(false);
      });
    });

    describe('when was disliked previously', () => {
      let rating: Rating;
      const testId = 123;
      beforeEach(() => {
        rating = new Rating();

        rating.dislike(testId);
      });

      it('removes id from .byId', () => {
        rating.dislike(testId);

        expect(rating.byId[testId]).toBe(undefined);
      });
    });

    describe('when was liked previously', () => {
      let rating: Rating;
      const testId = 123;
      beforeEach(() => {
        rating = new Rating();

        rating.like(testId);
      });

      it('overrides id in .byId', () => {
        rating.dislike(testId);

        expect(rating.byId[testId]).toBe(false);
      });
    });
  });
});
