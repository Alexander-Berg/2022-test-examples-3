import {useMemo, useState} from 'react';

import {TUaasExperiments} from 'server/providers/experiments/utils/mapExperimentsConfig';

interface IUseSearchParams {
    experiments: TUaasExperiments | null;
}

interface IUseSearch {
    text: string;
    setText(text: string): void;
    filteredExperiments: [string, string][];
}

export default function useSearch({experiments}: IUseSearchParams): IUseSearch {
    const [text, setText] = useState('');

    const filteredExperiments = useMemo(() => {
        if (!experiments) {
            return [];
        }

        if (!text) {
            return Object.entries(experiments);
        }

        return Object.entries(experiments).filter(
            ([expConfigName, expConfigString]) => {
                const textLowerCase = text.toLowerCase();

                return (
                    expConfigName.toLowerCase().includes(textLowerCase) ||
                    expConfigString.toLowerCase().includes(textLowerCase)
                );
            },
        );
    }, [experiments, text]);

    return {
        text,
        setText,
        filteredExperiments,
    };
}
