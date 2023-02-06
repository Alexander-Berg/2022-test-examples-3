import React from 'react';
import { makeOrder, getTopFakes, getBottomFakes } from 'lib/direct/fake';
import { getClassById } from 'lib/direct/dynamic-classes';

jest.mock('lib/direct/dynamic-classes');

describe('src/lib/direct/fake', () => {
    beforeEach(() => {
        jest.spyOn(Math, 'random').mockReturnValue(0.4);
        getClassById.mockImplementation((id) => `${id}-random-name`);
    });

    describe('#makeOrder', () => {
        it('should return generated order', () => {
            expect(makeOrder()).toEqual([false, false]);
        });
    });

    describe('#getTopFakes', () => {
        it('should generate top fakes with random order', () => {
            expect(getTopFakes()).toEqual([
                <div key="top-0-random-name" className="top-0-random-name"/>,
                <div key="top-1-random-name" className="top-1-random-name"/>
            ]);
        });

        it('should generate top fakes with specified order', () => {
            expect(getTopFakes([true])).toEqual([
                <span key="top-0-random-name" className="top-0-random-name"/>
            ]);
        });
    });

    describe('#getBottomFakes', () => {
        it('should generate bottom fakes with random order', () => {
            expect(getBottomFakes()).toEqual([
                <div key="bottom-0-random-name" className="bottom-0-random-name"/>,
                <div key="bottom-1-random-name" className="bottom-1-random-name"/>
            ]);
        });

        it('should generate bottom fakes with specified order', () => {
            expect(getBottomFakes([true])).toEqual([
                <span key="bottom-0-random-name" className="bottom-0-random-name"/>
            ]);
        });
    });
});
