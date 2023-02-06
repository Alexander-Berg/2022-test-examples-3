#pragma once

#include "metadata.h"

#include <market/idx/datacamp/proto/offer/OfferMeta.pb.h>
#include <market/idx/datacamp/proto/offer/OfferStatus.pb.h>

#include <util/system/mutex.h>
#include <library/cpp/testing/unittest/gtest.h>

const Market::DataCamp::Flag* FindFlag(const Market::DataCamp::OfferStatus& status, Market::DataCamp::DataSource source);

template<class TDatacampOffer>
void CheckFlag(const TDatacampOffer& offer, Market::DataCamp::DataSource source, bool value) {
    const Market::DataCamp::Flag* flag = FindFlag(offer.status(), source);
    if (value) {
        ASSERT_NE(flag, nullptr);
        ASSERT_EQ(flag->flag(), value);
    } else {
        if (flag != nullptr) {
            ASSERT_EQ(flag->flag(), value);
        }
    }
}

template<typename T = Market::DataCamp::Offer, typename A = NMiner::TDatacampOffer>
A MakeDefault() {
    static TVector<T> protos;
    static TMutex lock;
    T* proto = nullptr;
    with_lock(lock) {
        protos.emplace_back();
        proto = &protos.back();
    }
    return A(proto);
}

template<typename T = Market::DataCamp::Offer, typename A = NMiner::TDatacampOffer>
A MakeBasicService() {
    static TVector<T> basics;
    static TVector<T> services;
    static TMutex lock2;
    T* basicPtr = nullptr;
    T* servicePtr = nullptr;
    with_lock(lock2) {
        basics.emplace_back();
        basicPtr = &basics.back();
        services.emplace_back();
        servicePtr = &services.back();
    }
    return A(basicPtr, servicePtr);
}
