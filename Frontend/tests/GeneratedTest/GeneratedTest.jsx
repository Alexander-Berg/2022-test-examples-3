import React, { useCallback, useEffect } from 'react';
import { animated, useSpring } from '@react-spring/web';
import { useDrag } from '@use-gesture/react';
import { cn } from '@bem-react/classname';
import './GeneratedTest.css';

import { useTestController } from 'hooks/useTestController';

import CorrectIcon from '../../../assets/tests/icons/swipeCorrect.svg';
import WrongIcon from '../../../assets/tests/icons/swipeWrong.svg';

import { ProgressBar } from '../ProgressBar/ProgressBar';

const b = cn('GeneratedTest');

const isTouchLayout = window.innerWidth <= 768;

const MOVEMENT_TO_ROTATION_COEF = 0.1;
const X_SWIPE_ANIMATION_DISTANCE = isTouchLayout ? 300 : 600;
const Z_SWIPE_ANIMATION_ROTATION = isTouchLayout ? 30 : 45;
const TASK_SWITCH_DELAY = 500;

const useDragConfig = {
    swipe: {
        duration: 2000,
        distance: 10,
    },
};

export const GeneratedTest = ({
    tasks,

    checkAnswer,
    onTestFinished,
}) => {
    const {
        question,
        options,
        score,
        result,
        userAnswerKey,
        currentQuestion, totalQuestions,

        onOptionSelect,
    } = useTestController({
        tasks,
        checkAnswer,
        onTestFinished,
        taskSwitchMode: 'auto',
        taskSwitchDelay: TASK_SWITCH_DELAY,
    });

    const [styles, api] = useSpring(() => ({ from: { x: 0, y: 0, rotateZ: 0, opacity: 1 }, immediate: true }));

    const handleOptionSelect = useCallback(index => {
        onOptionSelect(options[index]);
    }, [onOptionSelect, options]);

    useEffect(() => {
        if (userAnswerKey) {
            const direction = options.findIndex(({ key }) => key === userAnswerKey) === 0 ? -1 : 1;

            api({
                x: X_SWIPE_ANIMATION_DISTANCE * direction,
                rotateZ: Z_SWIPE_ANIMATION_ROTATION * direction,
                opacity: 0,
            });
        }
    }, [result]);

    useEffect(() => {
        api({
            x: 0,
            y: 0,
            rotateZ: 0,
            opacity: 1,
            immediate: true,
        });
    }, [question]);

    const bind = useDrag(e => {
        if (e.swipe[0]) {
            return handleOptionSelect(e.swipe[0] === -1 ? 0 : 1);
        }

        api({
            x: e.down ? e.movement[0] : 0,
            y: e.down ? e.movement[1] : 0,
            rotateZ: e.down ? e.movement[0] * MOVEMENT_TO_ROTATION_COEF : 0,
        });
    }, useDragConfig);

    // eslint-disable-next-line no-nested-ternary
    const highlight = result ?
        result.isCorrect ? 'correct' : 'incorrect' :
        false;

    return (
        <div className={ b() }>
            <div className={ b('Progress') }>
                <div className={ b('Score') }>Очки: { score }</div>
                <ProgressBar progress={ ((currentQuestion - 1) / totalQuestions) * 100 } />
            </div>

            <div className={ b('Question') }>
                <div className={ b('Text') }>
                    { question.text }
                </div>
            </div>

            <div className={ b('Quotes') }>
                <div className={ b('BackQuote') } />
                <div className={ b('MiddleQuote') } />
                <animated.div style={ styles } { ...bind() } className={ b('Quote') }>
                    { question.quote }
                </animated.div>
                <div className={ b('QuotesOverlay', { highlight }) }>
                    { highlight === 'correct' && <CorrectIcon className={ b('Icon') } /> }
                    { highlight === 'incorrect' && <WrongIcon className={ b('Icon') } /> }
                </div>
            </div>

            <div className={ b('Options') }>
                { options.map((option, idx) =>
                    <div
                        className={ b('Option', { highlighted: result && userAnswerKey === option.key, correct: result && result.isCorrect }) }
                        key={ idx }
                        onClick={ () => handleOptionSelect(idx) }
                    >
                        { option.text }
                    </div>,
                )}
            </div>
        </div>
    );
};
