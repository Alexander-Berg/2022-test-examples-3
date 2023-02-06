#pragma once

#include <string>
#include <boost/fusion/adapted/struct/define_struct.hpp>
#include <boost/optional.hpp>
#include "wrapper.h"


BOOST_FUSION_DEFINE_STRUCT(, GeneralParams,
    (int, integer)
    (std::string, str)
    (double, dob)
    (unsigned, uns)
    (bool, boolean)
    (std::optional<std::string>, stdOpt)
    (boost::optional<std::string>, boostOpt)
    (std::vector<std::string>, strs)
)

BOOST_FUSION_DEFINE_STRUCT(, WithInt,
    (int, foo)
)

BOOST_FUSION_DEFINE_STRUCT(, WithBool,
    (bool, bar)
)

BOOST_FUSION_DEFINE_STRUCT(, WithUnsigned,
    (unsigned char, unsCh)
)

BOOST_FUSION_DEFINE_STRUCT(, WithTimeT,
    (std::time_t, time)
)

BOOST_FUSION_DEFINE_STRUCT(, WithOptional,
    (std::optional<int>, optInt)
)

BOOST_FUSION_DEFINE_STRUCT(, NestedWithOptional,
    (std::optional<WithOptional>, optStruct)
    (float, flo)
)

BOOST_FUSION_DEFINE_STRUCT(, GeneralWithOptional,
    (std::optional<NestedWithOptional>, nested)
)

BOOST_FUSION_DEFINE_STRUCT(, WithSequences,
    (std::vector<double>, array)
)

BOOST_FUSION_DEFINE_STRUCT(, WithPointers,
    (std::shared_ptr<double>, pDouble)
    (boost::shared_ptr<std::string>, pStr)
    (boost::shared_ptr<WithTimeT>, pTimet)
)

BOOST_FUSION_DEFINE_STRUCT(, WithRawPointer,
    (int*, pInt)
)

BOOST_FUSION_DEFINE_STRUCT(, StructForTags,
    (Header<std::string>, header)
    (Get<int>, get)
    (Post<unsigned>, post)
    (bool, untagged)
)

BOOST_FUSION_DEFINE_STRUCT(, BigStructWithTags,
    (std::vector<Header<std::string>>, headers)
    (std::shared_ptr<Get<std::string>>, getPtr)
    (std::optional<Post<std::string>>, postOpt)
    (StructForTags, st)
)
