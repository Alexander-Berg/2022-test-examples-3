#pragma once

#include <kernel/doom/offroad_attributes_wad/offroad_attributes_wad_io.h>
#include <kernel/doom/wad/mega_wad_writer.h>
#include <kernel/doom/wad/wad.h>

namespace NTestShard {

using TKeySearcher = NDoom::TOffroadAttributesKeyIo::TSearcher;
using TKeyIterator = TKeySearcher::TIterator;
using TKeyData = TKeySearcher::TKeyData;
using THitSearcher = NDoom::TOffroadAttributesHitIo::TSearcher;
using THitIterator = THitSearcher::TIterator;
using THit = THitSearcher::THit;

}
