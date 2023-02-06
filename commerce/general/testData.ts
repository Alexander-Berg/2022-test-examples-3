export interface ISection {
    serviceId: number,
    code: string,
    title: string
}

export interface IAnswer {
    id: number,
    active: number,
    questionId: IQuestion,
    text: string,
    correct: number
}

export interface ICategory {
    id: number,
    difficulty: number,
    timeLimit: number
}

export interface IQuestion {
    id: number,
    active: number,
    sectionId: ISection,
    categoryId: ICategory,
    text: string,
    type: number
}

export interface ITrialTemplateAllowedFails {
    allowedFails: number,
    sectionId: ISection,
    trialTemplateId: number
}

export interface ITrialTemplateToSections {
    quantity: number,
    categoryId: ICategory,
    sectionId: ISection,
    trialTemplateId: number
}

interface ITestData {
    sections: ISection[],
    answers: IAnswer[],
    categories: ICategory[],
    questions: IQuestion[],
    trialTemplateAllowedFails: ITrialTemplateAllowedFails[],
    trialTemplateToSections: ITrialTemplateToSections[]
}

export default ITestData;
