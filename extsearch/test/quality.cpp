#include "quality.h"

using namespace NImages::NLinkSelector;

void TLinkRocQualityCounter::CountPositiveSetSize() {
    PositiveSetSize = 0;
    for (size_t i = 0; i < RankedSet.size(); i++) {
        PositiveSetSize += (RankedSet[i].Truth == 1.f ? 1 : 0);
    }
}

TLinkRocQualityCounter::TLinkRocQualityCounter(const TRankAnswer& input)
    : RankedSet(input)
{
    RankedSet.SortDataByResult();
    CountPositiveSetSize();
}

TVector<TPoint> TLinkRocQualityCounter::FilterPoints(const TVector<TPoint>& points) const {
    float step = (points.back().X - points[0].X) / (float)(PointsNumber - 2);
    float currentStepElement = points[0].X + step;
    size_t currentPoint = 1;
    TVector<TPoint> result;
    result.push_back(points[0]);

    for (size_t i = 1; i < PointsNumber - 1; i++) {
        while (points[currentPoint].X < currentStepElement) {
            currentPoint++;
        }
        if (currentStepElement - points[currentPoint - 1].X < points[currentPoint].X - currentStepElement) {
            result.push_back(points[currentPoint - 1]);
        } else {
            result.push_back(points[currentPoint]);
        }
        currentStepElement += step;
    }
    result.push_back(points.back());
    return result;
}

TLinkRocQualityCounter::TCurvePosition::TCurvePosition(const TRankAnswer& data) {
    for (size_t i = 0; i < data.size(); i++) {
        if (data[i].Truth == 1.f) {
            TruePositiveCount++;
        } else {
            FalsePositiveCount++;
        }
    }
}

void TLinkRocQualityCounter::TCurvePosition::Next(float element) {
    if (element == 1.f) {
        TruePositiveCount--;
        FalseNegativeCount++;
    } else {
        FalsePositiveCount--;
        TrueNegativeCount++;
    }
}

float TLinkRocQualityCounter::TCurvePosition::CountTruePositiveRate() const {
    return (float)TruePositiveCount / (float)(TruePositiveCount + FalseNegativeCount);
}

float TLinkRocQualityCounter::TCurvePosition::CountFalsePositiveRate() const {
    return (float)FalsePositiveCount / (float)(FalsePositiveCount + TrueNegativeCount);
}

TVector<TPoint> TLinkRocQualityCounter::GetRocCurve() const {
    TVector<TPoint> curvePoints;
    TCurvePosition curvePosition(RankedSet);
    for (size_t i = 0; i < RankedSet.size(); i++) {
        curvePosition.Next(RankedSet[i].Truth);
        curvePoints.push_back(TPoint(curvePosition.CountTruePositiveRate(),
                                     curvePosition.CountFalsePositiveRate()));
    }
    Reverse(curvePoints.begin(), curvePoints.end());
    return curvePoints;
    return FilterPoints(curvePoints);
}

TLinkRankerQualityCounter::TLinkRankerQualityCounter(const TRankAnswer& input)
    : RankedSet(input)
{
    RankedSet.SortDataByResult();
}

float TLinkRankerQualityCounter::GetPairNumber() const {
    return (float)(RankedSet.size() * (RankedSet.size() - 1) / 2);
}

float TLinkRankerQualityCounter::GetInversionQuality() const {
    size_t inversionCounter = 0;
    for (size_t i = 0; i < RankedSet.size(); i++) {
        for (size_t j = i + 1; j < RankedSet.size(); j++) {
            if (RankedSet[j].LessByTruth(RankedSet[i])) {
                inversionCounter++;
            }
        }
    }
    return ((float)inversionCounter / GetPairNumber());
}

float TLinkRankerQualityCounter::WeightOfElement(float x) {
    return (log(x + 1.f));
}

float TLinkRankerQualityCounter::GetDCGQuality() const {
    float dcg = 0.f;
    float maxDcg = 0.f;
    for (int i = (int)(RankedSet.size() - 1); i >= 0; i--) {
        dcg += (pow(2.f, RankedSet[i].Truth) - 1.f) / WeightOfElement(RankedSet.size() - i);
    }

    TVector<float> truth;
    for (size_t i = 0; i < RankedSet.size(); i++) {
        truth.push_back(RankedSet[i].Truth);
    }
    Sort(truth.begin(), truth.end());
    for (int i = (int)(RankedSet.size() - 1); i >= 0; i--) {
        maxDcg += (pow(2.f, truth[i]) - 1.f) / WeightOfElement(RankedSet.size() - i);
    }
    return dcg / maxDcg;
}
