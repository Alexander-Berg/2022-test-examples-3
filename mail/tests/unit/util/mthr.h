#pragma once

#include <mail/notsolitesrv/src/mthr/types/request.h>
#include <mail/notsolitesrv/src/mthr/types/response.h>

#include <yamail/data/reflection/reflection.h>

#include <algorithm>
#include <cstdint>
#include <string>
#include <vector>

namespace NNotSoLiteSrv::NMthr {

struct TTestDataFrom {
    std::string Domain;
};

struct TTestDataMessage {
    std::vector<std::string> References;
    std::string InReplyTo;
    TTestDataFrom From;
    std::string MessageId;
    std::string Subject;
};

struct TTestDataRequest {
    TTestDataMessage Message;
    std::vector<uint32_t> Types;
    std::string DomainLabel;
};

struct TTestDataHash {
    std::string Namespace;
    std::string Value;
};

struct TTestDataLimits {
    uint32_t CountLimit;
    uint32_t DaysLimit;
};

struct TTestDataOut {
    TTestDataHash Hash;
    TTestDataLimits Limits;
    std::vector<std::string> ReferenceHashes;
    std::string InReplyToHash;
    std::string Rule;
    std::string MessageIdHash;
    std::vector<std::string> MessageIds;
};

struct TTestData {
    TTestDataRequest Request;
    TTestDataOut Out;
};

inline bool operator==(const TTestDataHash& testDataHash, const TThreadHash& threadHash) {
    return
        (testDataHash.Namespace == threadHash.Namespace) &&
        (testDataHash.Value == threadHash.Value);
}

inline bool operator==(const TTestDataLimits& testDataLimits, const TThreadLimits& threadLimits) {
    return
        (testDataLimits.CountLimit == threadLimits.Count) &&
        (testDataLimits.DaysLimit == threadLimits.Days);
}

inline bool operator==(const TTestDataOut& out, const TMthrResponse& response) {
    return
        (out.Hash == response.ThreadInfo.Hash) &&
        (out.Limits == response.ThreadInfo.Limits) &&
        std::is_permutation(out.ReferenceHashes.begin(), out.ReferenceHashes.end(),
            response.ThreadInfo.ReferenceHashes.begin()) &&
        (out.InReplyToHash == response.ThreadInfo.InReplyToHash) &&
        (out.Rule == response.ThreadInfo.Rule) &&
        (out.MessageIdHash == response.ThreadInfo.MessageIdHash) &&
        std::is_permutation(out.MessageIds.begin(), out.MessageIds.end(),
            response.ThreadInfo.MessageIds.begin());
}

inline bool operator==(const TMthrRequest& left, const TMthrRequest& right) {
    return
        (left.HdrFromDomain == right.HdrFromDomain) &&
        (left.Subject == right.Subject) &&
        (left.MsgTypes == right.MsgTypes) &&
        (left.MessageId == right.MessageId) &&
        (left.References == right.References) &&
        (left.InReplyTo == right.InReplyTo) &&
        (left.DomainLabel == right.DomainLabel);
}

}

YREFLECTION_ADAPT_ADT(NNotSoLiteSrv::NMthr::TTestDataFrom,
    YREFLECTION_WO_MEMBER_RENAMED(std::string, domain, Domain)
)

YREFLECTION_ADAPT_ADT(NNotSoLiteSrv::NMthr::TTestDataMessage,
    YREFLECTION_WO_MEMBER_RENAMED(std::vector<std::string>, references, References)
    YREFLECTION_WO_MEMBER_RENAMED(std::string, in_reply_to, InReplyTo)
    YREFLECTION_WO_MEMBER_RENAMED(NNotSoLiteSrv::NMthr::TTestDataFrom, from, From)
    YREFLECTION_WO_MEMBER_RENAMED(std::string, message_id, MessageId)
    YREFLECTION_WO_MEMBER_RENAMED(std::string, subject, Subject)
)

YREFLECTION_ADAPT_ADT(NNotSoLiteSrv::NMthr::TTestDataRequest,
    YREFLECTION_WO_MEMBER_RENAMED(NNotSoLiteSrv::NMthr::TTestDataMessage, message, Message)
    YREFLECTION_WO_MEMBER_RENAMED(std::vector<uint32_t>, types, Types)
    YREFLECTION_WO_MEMBER_RENAMED(std::string, domain_label, DomainLabel)
)

YREFLECTION_ADAPT_ADT(NNotSoLiteSrv::NMthr::TTestDataHash,
    YREFLECTION_WO_MEMBER_RENAMED(std::string, namespace, Namespace)
    YREFLECTION_WO_MEMBER_RENAMED(std::string, value, Value)
)

YREFLECTION_ADAPT_ADT(NNotSoLiteSrv::NMthr::TTestDataLimits,
    YREFLECTION_WO_MEMBER_RENAMED(uint32_t, count_limit, CountLimit)
    YREFLECTION_WO_MEMBER_RENAMED(uint32_t, days_limit, DaysLimit)
)

YREFLECTION_ADAPT_ADT(NNotSoLiteSrv::NMthr::TTestDataOut,
    YREFLECTION_WO_MEMBER_RENAMED(NNotSoLiteSrv::NMthr::TTestDataHash, hash, Hash)
    YREFLECTION_WO_MEMBER_RENAMED(NNotSoLiteSrv::NMthr::TTestDataLimits, limits, Limits)
    YREFLECTION_WO_MEMBER_RENAMED(std::vector<std::string>, reference_hashes, ReferenceHashes)
    YREFLECTION_WO_MEMBER_RENAMED(std::string, in_reply_to_hash, InReplyToHash)
    YREFLECTION_WO_MEMBER_RENAMED(std::string, rule, Rule)
    YREFLECTION_WO_MEMBER_RENAMED(std::string, message_id_hash, MessageIdHash)
    YREFLECTION_WO_MEMBER_RENAMED(std::vector<std::string>, message_ids, MessageIds)
)

YREFLECTION_ADAPT_ADT(NNotSoLiteSrv::NMthr::TTestData,
    YREFLECTION_WO_MEMBER_RENAMED(NNotSoLiteSrv::NMthr::TTestDataRequest, request, Request)
    YREFLECTION_WO_MEMBER_RENAMED(NNotSoLiteSrv::NMthr::TTestDataOut, out, Out)
)
