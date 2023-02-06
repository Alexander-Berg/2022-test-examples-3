#pragma once
#include <search/web/configurable_rearrange/graph_applier.h>

namespace NConfigurableRearrange {

    class TExportDataTester {
      public:
        TExportDataTester(const TExportData& data)
          : data(data) {
        }

        bool RunAllTests(IOutputStream*) const;

        bool TestBuild(IOutputStream*) const;
        bool TestUniqueGraphNames(IOutputStream*) const;
        bool TestUniquePatchNames(IOutputStream*) const;
        bool TestGraphIsCorrect(IOutputStream*) const;
        bool TestPatchesOnUniqueApplication(IOutputStream*) const;

        bool TestPatchesOnProductionGraphs(IOutputStream*) const;
        bool TestAutonomyOfNamespaces(IOutputStream*) const;

      private:
        const TExportData& data;
    };
}
