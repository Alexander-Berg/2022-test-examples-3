#include "test_utils.h"

using namespace NGeoDB;

namespace NRouteWizardProxy {

    const TCateg MOSCOW_ID = 213;
    const auto MOSCOW = GetRegion(MOSCOW_ID);

    const TCateg RUSSIA_ID = 225;
    const auto RUSSIA = GetRegion(RUSSIA_ID);

    const TCateg SPB_ID = 2;
    const auto SPB = GetRegion(SPB_ID);

    const auto INVALID_REGION = GetRegion(END_CATEG);

    const TGeoKeeper& GetGeoDB() {
        static const auto geoDB = TGeoKeeper::LoadToHolder("geodb.bin");
        return *geoDB;
    }

    NGeoDB::TGeoPtr GetRegion(TCateg id) {
        return GetGeoDB().Find(id);
    }

} // NRouteWizardProxy
