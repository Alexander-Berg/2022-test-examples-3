import { CardSchema } from 'schema/card/CardSchema';

import { CARDS_BANNED } from 'spec/stubs/cards/banned';
import { CARDS_GEO } from 'spec/stubs/cards/geo';
import { CARDS_GIF } from 'spec/stubs/cards/gif';
import { CARDS_HASH_TAG } from 'spec/stubs/cards/hashtag';
import { CARDS_IMAGES } from 'spec/stubs/cards/images';
import { CARDS_LINKS } from 'spec/stubs/cards/links';
import { CARDS_ORGANIZATIONS } from 'spec/stubs/cards/organizations';
import { CARDS_PRODUCTS } from 'spec/stubs/cards/products';
import { CARDS_RICH_GAMES } from 'spec/stubs/cards/richGames';
import { CARDS_RICH_GEO } from 'spec/stubs/cards/richGeo';
import { CARDS_RICH_MARKET } from 'spec/stubs/cards/richMarket';
import { CARDS_RICH_ORGANIZATIONS } from 'spec/stubs/cards/richOrganizations';
import { CARDS_RICH_RECIPES } from 'spec/stubs/cards/richRecipes';
import { CARDS_SERIES } from 'spec/stubs/cards/series';
import { CARDS_VIDEOS } from 'spec/stubs/cards/videos';

// @ts-ignore
export const CARD_DEFAULT: CardSchema = {
    has_duplicate: false,
    id: '',
    is_liked: false,
    is_marked: false,
    is_private: false,
    labels: [],
    source_meta: {
        page_url: '',
        page_domain: '',
    },
    stat: {
        shares_count: 0,
        likes_count: 0,
    },
};

export const CARDS = {
    ...CARDS_IMAGES,
    ...CARDS_LINKS,
    ...CARDS_SERIES,
    ...CARDS_RICH_RECIPES,
    ...CARDS_RICH_GAMES,
    ...CARDS_RICH_GEO,
    ...CARDS_GEO,
    ...CARDS_RICH_MARKET,
    ...CARDS_RICH_ORGANIZATIONS,
    ...CARDS_ORGANIZATIONS,
    ...CARDS_BANNED,
    ...CARDS_PRODUCTS,
    ...CARDS_HASH_TAG,
    ...CARDS_VIDEOS,
    ...CARDS_GIF,
};
