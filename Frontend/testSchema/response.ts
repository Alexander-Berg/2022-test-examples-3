export interface ResponseUserSchema {
    id: string;
    login: string;
    public_id: string;
}

export interface ResponseBoardSchema {
    id: string;
    slug: string;
    owner: string;
}

export interface ResponseCardSchema {
    id: string;
}

export interface Response {
    users: ResponseUserSchema[];
    boards: ResponseBoardSchema[];
    cards: ResponseCardSchema[];
}
