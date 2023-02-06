import tap from 'lodash/tap';
import React from 'react';

import { Props } from 'client/components/problem-test/types';
import ProblemTestControls from 'client/components/problem-test/__controls';
import ProblemTestVariants from 'client/components/problem-test/__variants';

const ProblemTest = ({ variants, answer, multi = true, disabled = false, onUpdate }: Props) => {
    const onAddVariant = (value: string) => onUpdate({ answer, variants: [...variants, value] });
    const onRemoveVariant = (idx: number) => {
        onUpdate({
            answer: answer
                // Убираем правильный вариант если выбран
                .filter((answerIdx) => answerIdx !== idx)
                // Все остальные дальше правильные ответы смещаются на один
                .map((answerIdx) => (answerIdx < idx ? answerIdx : answerIdx - 1)),
            variants: tap([...variants], (variantsCopy) => variantsCopy.splice(idx - 1, 1)),
        });
    };

    const onChangeCorrectness = (idx: number, value: boolean) => {
        if (!multi) {
            return onUpdate({ answer: [idx], variants });
        }

        return value
            ? onUpdate({ answer: [...answer, idx].sort((a, b) => a - b), variants })
            : onUpdate({ answer: answer.filter((answerIdx) => answerIdx !== idx), variants });
    };

    return (
        <div>
            <ProblemTestVariants
                onRemove={onRemoveVariant}
                onChange={onChangeCorrectness}
                answer={answer}
                variants={variants}
                disabled={disabled}
            />
            <ProblemTestControls onAdd={onAddVariant} disabled={disabled} />
        </div>
    );
};

export default ProblemTest;
