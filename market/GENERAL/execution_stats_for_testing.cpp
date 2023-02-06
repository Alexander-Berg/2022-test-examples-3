#include <market/library/execution_stats/execution_stats.h>
#include <market/library/execution_stats/execution_stats_collector.h>

namespace NExecStats {

TExecutionStats& TExecutionStatsCollector::GetImplForTesting() {
    return *Impl;
}

void TExecutionStats::SetHostNameForTesting(const TString& hostName) {
    HostName = hostName;
}

}  // namespace NExecStats
