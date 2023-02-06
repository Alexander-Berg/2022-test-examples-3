import React from 'react';

import {IOfferToken} from 'server/api/HotelsBookAPI/types/ITestBookOfferToken';

import {hotelsURLs} from 'projects/hotels/utilities/urls';

import Flex from 'components/Flex/Flex';
import Link from 'components/Link/Link';

interface IOfferListProps {
    offerTokens: IOfferToken[];
}

const OfferList: React.FC<IOfferListProps> = props => {
    const {offerTokens} = props;

    return (
        <Flex flexDirection="column" between={2}>
            {offerTokens.map((offerToken, index) => (
                <Link
                    key={index}
                    target="_blank"
                    to={hotelsURLs.getBookPage({token: offerToken.token})}
                >
                    {offerToken.offerName}
                </Link>
            ))}
        </Flex>
    );
};

export default OfferList;
