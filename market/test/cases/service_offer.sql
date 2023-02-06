/* syntax version 1 */
SELECT
  SOffers::ParseServiceOfferProfile(AsStruct(
        YabsId as YabsId,
        BusinessId as BusinessId,
        ShopId as ShopId,
        WarehouseId as WarehouseId,
        Codec as Codec,
        Offer as Offer,
      )) as ServiceOffer,
    SOffers::CalculateServiceOfferProfileSize(AsStruct(
        YabsId as YabsId,
        BusinessId as BusinessId,
        ShopId as ShopId,
        WarehouseId as WarehouseId,
        Codec as Codec,
        Offer as Offer,
      )) as FieldSizes,
FROM Input;
