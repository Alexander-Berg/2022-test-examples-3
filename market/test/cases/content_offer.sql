/* syntax version 1 */
SELECT
  SOffers::ParseContentOfferProfile(AsStruct(
        YabsId as YabsId,
        BusinessId as BusinessId,
        Codec as Codec,
        Offer as Offer,
      )) as ContentOffer,
  SOffers::CalculateContentOfferProfileSize(AsStruct(
        YabsId as YabsId,
        BusinessId as BusinessId,
        Codec as Codec,
        Offer as Offer,
      )) as FieldSizes
FROM Input;
