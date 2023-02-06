#include <market/library/mrs/reader.h>

#include <util/system/event.h>

#include <stdio.h>
#include <chrono>

#include <pthread.h>

#include <thread>

bool Get(ui32* region, ui64* model, FILE* input) {
    char str[100];
    if (fgets(str, sizeof(str), input)) {
        return sscanf(str, "%lu %u", model, region) >= 2;
    }
    return false;
}

template<class Reader>
void CheckModels(const ui32& region, const TVector<ui64>& models, Reader& reader, ui32 core, TAutoEvent& qEvent, TAutoEvent& aEvent, bool& finish, ui64& result) {
    cpu_set_t cpuset;
    CPU_ZERO(&cpuset);
    CPU_SET(core, &cpuset);
    pthread_setaffinity_np(pthread_self(), sizeof(cpu_set_t), &cpuset);
    while (true) {
        qEvent.Wait();
        if (finish)
            return;
        auto start = std::chrono::high_resolution_clock::now();
        for (ui64 model: models) {
            auto entry = reader->Find(model, region);
            Y_UNUSED(entry);
        }
        auto end = std::chrono::high_resolution_clock::now();
        result = std::chrono::duration_cast<std::chrono::microseconds>(end - start).count();
        aEvent.Signal();
    }
}

template<class Reader1, class Reader2>
void TestPerf(FILE* input, Reader1& reader1, Reader2& reader2) {
    ui32 region;
    ui64 model;
    TVector<ui64> models;
    bool av = Get(&region, &model, input);
    TAutoEvent qEvent1, qEvent2, aEvent1, aEvent2;
    bool finish = false;
    ui64 time1, time2;
    ui32 sregion;
    std::thread thr1([&](){CheckModels(sregion, models, reader1, 24, qEvent1, aEvent1, finish, time1);});
    std::thread thr2([&](){CheckModels(sregion, models, reader2, 25, qEvent2, aEvent2, finish, time2);});
    while (av) {
        models.clear();
        sregion = region;
        do {
            models.push_back(model);
            av = Get(&region, &model, input);
        } while (av && (region == sregion));
        qEvent1.Signal();
        aEvent1.Wait();
        qEvent2.Signal();
        aEvent2.Wait();
        fprintf(stderr, "Region: %u time1: %lu time2: %lu model count %lu\n", sregion, time1, time2, models.size());
    }
    finish = true;
    qEvent1.Signal();
    qEvent2.Signal();
    thr1.join();
    thr2.join();
}

template<class Reader1, class Reader2>
void TestDiff(FILE* input, Reader1& reader1, Reader2& reader2) {
    char str[100];
    while (fgets(str, sizeof(str), input)) {
        ui32 region;
        ui64 model_id;
        if (sscanf(str, "%lu %u", &model_id, &region) >= 2) {
            NMarket::NModelRegionalStats::TStatsEntry entry1 = reader1->Find(model_id, region);
            NMarket::NModelRegionalStats::TStatsEntry entry2 = reader2->Find(model_id, region);
            if (!(entry1 == entry2)) {
                fprintf(stderr, "Different entries for model %lu region %u\n", model_id, region);
//            } else {
//                fprintf(stderr, "Equal entries for model %u region %u\n", model_id, region);
            }
        } else {
            fprintf(stderr, "Wrong input line %s\n", str);
        }
    }
}

int main(int, char** argv) {
    auto reader1 = NMarket::NModelRegionalStats::CreateReader(Market::NMmap::IMemoryRegion::MmapFile(argv[1]));
    auto reader2 = NMarket::NModelRegionalStats::CreateReader(Market::NMmap::IMemoryRegion::MmapFile(argv[2]));
    FILE* input = fopen(argv[3], "r");
    if (argv[4]) {
        TestDiff(input, reader1, reader2);
    } else {
        TestPerf(input, reader1, reader2);
    }
    return 0;

}
