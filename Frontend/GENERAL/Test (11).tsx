import React, { useCallback, useState } from 'react';

import { CategoryTest } from 'components/Tests/CategoryTest/CategoryTest';
import { TestResult } from 'components/Tests/TestResult/TestResult';

import data from './data.json';
import { urls } from '../../../urls';

import type { Categories } from 'hooks/useCategoryTestController/types';

const TOUCH_LAYOUT_WIDTH = 768;

const isTouchLayout = window.innerWidth <= TOUCH_LAYOUT_WIDTH;

interface IResult {
    score: number;
    shortMessage: string;
    message: string;
    image: string;
}

const getResultData = (categories: Categories | null): IResult | null => {
    if (!categories) {
        return null;
    }

    let topScore: number = 0;
    let topCategory: string = '';

    Object.entries(categories).forEach(([category, score]) => {
        if (!topScore || score > topScore) {
            topScore = score;
            topCategory = category;
        }
    });

    const resultCategory = data.results
        .find(({ category }) => category === topCategory);

    return {
        score: topScore,
        shortMessage: resultCategory?.shortMessage || '',
        message: resultCategory?.message || '',
        image: (isTouchLayout ?
            resultCategory?.touchImage :
            resultCategory?.desktopImage
        ) || '',
    };
};

const tasks = data.tasks;

interface Props {
    onRetry: () => void;
}

export const IntoTheWildTest: React.FC<Props> = ({ onRetry }) => {
    const [finished, setFinished] = useState(false);
    const [categories, setCategories] = useState<Categories | null>(null);

    const onTestFinished = useCallback((categories: Categories) => {
        setFinished(true);
        setCategories(categories);
    }, []);

    const result = getResultData(categories);

    return (
        <>
            {finished ? (
                <TestResult
                    title="Тест: найдите облачного двойника вашего характера"
                    topImage={result?.image}
                    message={result?.message || ''}
                    testPath={urls.weather['you-are-a-cloud']}
                    onRetryButtonClick={onRetry}
                    isTransparent
                />
            ) : (
                <CategoryTest
                    tasks={tasks}
                    onTestFinished={onTestFinished}
                />
            )}
        </>
    );
};
