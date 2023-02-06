#pragma once

#include <crypta/idserv/data/graph.h>

NCrypta::NIS::TGraph CreateGraph(size_t nodesCount, size_t edgesCount, size_t base = 10000000);
NCrypta::NIS::TGraph CreateCompleteGraph(size_t nodesCount, size_t base = 10000000);
