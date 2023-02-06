#pragma once

#include <kernel/geodb/geodb.h>
#include <kernel/search_types/search_types.h>

namespace NRouteWizardProxy {

    extern const TCateg MOSCOW_ID;
    extern const NGeoDB::TGeoPtr MOSCOW;

    extern const TCateg RUSSIA_ID;
    extern const NGeoDB::TGeoPtr RUSSIA;

    extern const TCateg SPB_ID;
    extern const NGeoDB::TGeoPtr SPB;

    extern const NGeoDB::TGeoPtr INVALID_REGION;

    const NGeoDB::TGeoKeeper& GetGeoDB();
    NGeoDB::TGeoPtr GetRegion(TCateg id);

} // NRouteWizardProxy
