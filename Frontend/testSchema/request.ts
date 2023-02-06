import { BoardSchema } from 'schema/board/BoardSchema';
import { CardSchema } from 'schema/card/CardSchema';
import { UserSchema } from 'schema/user/UserSchema';

export interface RequestTestUserSchema {
    id: string;
    user: UserSchema;
}

export interface RequestTestBoardSchema {
    id: string;
    board: BoardSchema;
    owner?: string;
    coauthors?: RequestTestCoauthorSchema[];
}

export interface RequestTestCoauthorSchema {
    id: string;
    permission: number;
}

export interface RequestTestCardSchema {
    id: string;
    card: CardSchema;
    board?: string;
    owner?: string;
}

export interface Request {
    users: RequestTestUserSchema[];
    boards: RequestTestBoardSchema[];
    cards: RequestTestCardSchema[];
}
