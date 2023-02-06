/* eslint-disable no-magic-numbers */
import { BoardSchema } from 'schema/board/BoardSchema';

import { SourceType } from 'enums/sourceType';

import {
    BOARDS,
    BOARD_PRIVATE,
    BOARD_WISH_LIST,
} from 'spec/stubs/testSchema/boards';
import { CARDS } from 'spec/stubs/testSchema/cards';
import { Request } from 'spec/stubs/testSchema/request';
import { USERS } from 'spec/stubs/testSchema/users';

function createProfile(
    fixtureName: string,
    boards: BoardSchema[],
    cardFixtureNames?: string[],
    sourceType?: SourceType,
) {
    let profile: Request = {
        users: [{
            id: `${fixtureName}User`,
            user: USERS.USER_DEFAULT,
        }],
        boards: [],
        cards: [],
    };

    if (sourceType) {
        boards[0].card_source_type = sourceType;
    }

    for (let i = 0; i < boards.length; i++) {
        profile.boards.push({
            id: `${fixtureName}Board${i}`,
            board: boards[i],
            owner: `${fixtureName}User`,
        });
    }

    if (cardFixtureNames) {
        for (let j = 0; j < cardFixtureNames.length; j++) {
            profile.cards.push({
                id: `${fixtureName}BoardCard${j}`,
                board: `${fixtureName}Board0`,
                owner: `${fixtureName}User`,
                card: CARDS[cardFixtureNames[j]],
            });
        }
    }

    return profile;
}

export const PROFILE_DEFAULT = {
    users: [{
        id: 'profileDefault',
        user: USERS.USER_DEFAULT,
    }],
    boards: [],
    cards: [],
};

export const PROFILE_SETUP = {
    profile1: createProfile('profile1', BOARDS.slice(0, 1)),
    profile2: createProfile('profile2', BOARDS.slice(0, 7)),
    profile3: createProfile('profile3', BOARDS.slice(0, 26)),
    emptyBoard: createProfile('profile1', BOARDS.slice(0, 1)),
    boardLinks: createProfile('boardLinks', [BOARDS[0]], ['link1']),
    boardWishList: createProfile('boardWishList', [BOARD_WISH_LIST], ['link1']),
    boardWithThreeCards: createProfile('boardWithThreeCards', [BOARDS[0]], ['image1', 'image2', 'image3']),
    subscriptions: {
        users: [{
            id: 'subscriptionsUser',
            user: USERS.USER_DEFAULT,
        }, {
            id: 'subscriber1User',
            user: USERS.USER_DEFAULT,
        }, {
            id: 'subscriber2User',
            user: USERS.USER_DEFAULT,
        }],
        boards: [{
            id: 'subscriptionsBoard1',
            board: BOARDS[0],
            owner: 'subscriber1User',
        }, {
            id: 'subscriptionsBoard2',
            board: BOARDS[1],
            owner: 'subscriber2User',
        }],
    },
    twoBoardsOneCard: {
        users: [{
            id: 'twoBoardsOneCardUser',
            user: USERS.USER_DEFAULT,
        }],
        boards: [{
            id: 'twoBoardsOneCardBoard1',
            board: BOARDS[0],
            owner: 'twoBoardsOneCardUser',
        }, {
            id: 'twoBoardsOneCardBoard2',
            board: BOARDS[1],
            owner: 'twoBoardsOneCardUser',
        }],
        cards: [{
            id: 'twoBoardsOneCardCard1',
            board: 'twoBoardsOneCardBoard1',
            owner: 'twoBoardsOneCardUser',
            card: CARDS.image1,
        }, {
            id: 'twoBoardsOneCardCard2',
            board: 'twoBoardsOneCardBoard2',
            owner: 'twoBoardsOneCardUser',
            card: CARDS.image2,
        }],
    },
    oneBoardOneProductCard: createProfile('oneBoardOneProductCard', [BOARDS[0]], ['product1']),
    twoBoardsOneProductCard: createProfile('twoBoardsOneProductCard', [BOARDS[0], BOARDS[1]], ['product1']),
    twoBoardsNoCards: createProfile('twoBoardsNoCards', [BOARDS[0], BOARDS[1]]),
    oneBoardOneImageCard: createProfile('oneBoardOneImageCard', [BOARDS[0]], ['image1']),
    oneBoardTwoImageCards: createProfile('oneBoardTwoImageCards', [BOARDS[0]], ['image1', 'image2']),
    onePrivateBoard: createProfile('onePrivateBoard', [BOARD_PRIVATE]),
    oneBoardTwoTypesCards: createProfile('oneBoardTwoTypesCards', [BOARDS[2]], ['image2', 'link1']),
    oneBoardWithOneCardImageSeries: createProfile('oneBoardTwoTypesCards', [BOARDS[4]], ['series1']),
    boardImage: createProfile('boardImage', [BOARDS[0]], ['image1'], SourceType.IMAGE),
    boardRichRecipe: createProfile('boardRichRecipe', [BOARDS[0]], ['richRecipe1'], SourceType.IMAGE),
    boardSeries: createProfile('boardSeries', [BOARDS[0]], ['series1'], SourceType.IMAGE),
    boardGif: createProfile('boardGif', [BOARDS[0]], ['gif1'], SourceType.IMAGE),
    boardVideo: createProfile('boardVideo', [BOARDS[0]], ['video1'], SourceType.IMAGE),
    boardRichMarket: createProfile('boardRichMarket', [BOARDS[0]], ['richMarket1'], SourceType.IMAGE),
    boardRichGame: createProfile('boardRichGame', [BOARDS[0]], ['richGame1'], SourceType.IMAGE),
    boardRichGeo: createProfile('boardRichGeo', [BOARDS[0]], ['richGeo1'], SourceType.IMAGE),
    boardGeo: createProfile('boardGeo', [BOARDS[0]], ['Geo1'], SourceType.GEO),
    boardRichOrganization: createProfile('boardRichOrganization', [BOARDS[0]], ['richOrganization1'], SourceType.IMAGE),
    boardOrganization: createProfile('boardOrganization', [BOARDS[0]], ['organization1'], SourceType.ORGANIZATION),
    boardBanned: createProfile('boardBanned', [BOARDS[0]], ['banned1'], SourceType.IMAGE),
    boardFilm: createProfile('boardFilm', [BOARDS[0]], ['film1'], SourceType.FILM),
    boardTVSeries: createProfile('boardTVSeries', [BOARDS[0]], ['tvSeries1'], SourceType.SERIES),
    boardBook: createProfile('boardBook', [BOARDS[0]], ['book1'], SourceType.BOOK),
    boardLink: createProfile('boardLink', [BOARDS[0]], ['link1'], SourceType.LINK),
    privateBoard: createProfile('privateBoard', [BOARD_PRIVATE], ['link1']),
    wishlistBoard: createProfile('wishlistBoard', [BOARD_WISH_LIST], ['link1']),
    boardWithFourCards: createProfile('boardWithFourCards', [BOARDS[0]], ['image1', 'image2', 'image3', 'image4']),
    boardWithHashTags: createProfile('boardWithHashTags', [BOARDS[0]], ['hash1', 'hash2']),
};
