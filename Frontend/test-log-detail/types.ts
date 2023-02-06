import { TextAnswerViewMode } from 'client/components/hex-area/types';
import { ISubmissionTest, ISubmissionReport } from 'common/types/submission';
import { ICopyToClipboard } from 'client/store/client/types';

export interface Props {
    submissionId: ISubmissionReport['id'];
    test: ISubmissionTest;
    copyToClipboard: (_data: ICopyToClipboard) => void;
}

export interface ITabState {
    [key: string]: TextAnswerViewMode;
}

export type ViewTabData = {
    title: string;
    tab: ViewTabType;
    value: string;
    downloadUrl?: string;
};

export type ViewTabType = 'input' | 'output' | 'answer' | 'message' | 'error';
