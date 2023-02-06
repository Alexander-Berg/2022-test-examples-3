#include "testing_utils.h"

void InitRepContext(TRequestParams& rp) {
    rp.Report.Type = "templates_json";
    rp.GroupingParams.back().gMode = EGroupingMode::GM_DEEP;
    rp.GroupingParams.back().gAttr = "d";
    rp.SetMainGroupingAttr("d");
}
