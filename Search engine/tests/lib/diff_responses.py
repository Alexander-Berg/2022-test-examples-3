from __future__ import print_function

from tools.compare_protobufs.comparator import compare_protobufs

from google.protobuf.text_format import MessageToString


class ComparePrecisionContext:
    def __init__(self, fields_to_ignore=[], precision=1e-6):
        self.fields_to_ignore = set(fields_to_ignore)
        self.precision = precision
        self.embeddings_sum_error = 1e-2
        self.max_embeddings_diffs_count = 1


def _concat_slices(data_description, data_slice):
    slices = {}
    for index, descr in enumerate(data_description):
        slice_name = descr.SliceName
        features_ids = descr.FeaturesIds
        features_values = data_slice[index].Features
        assert len(features_ids) == len(features_values)
        if slice_name not in slices:
            slices[slice_name] = {}
        for feature_id, feature_value in zip(features_ids, features_values):
            slices[slice_name][feature_id] = feature_value
    sorted_slices = sorted(slices.items())
    concated_slices = []
    for slice_name, features in sorted_slices:
        concated_slice = []
        for _, feature_value in sorted(features.items()):
            concated_slice.append(feature_value)
        concated_slices.append(concated_slice)
    return concated_slices


def _concat_description(data_description):
    slices = {}
    for descr in data_description:
        slice_name = descr.SliceName
        features_ids = descr.FeaturesIds
        if slice_name not in slices:
            slices[slice_name] = set(features_ids)
        else:
            slices[slice_name].update(features_ids)
    sorted_slices = list(map(lambda kv: (kv[0], list(kv[1])), sorted(slices.items())))
    for slice_descr in sorted_slices:
        slice_descr[1].sort()
    return sorted_slices


def _concat_sliced_data(response):
    sliced_data_description = response.Web.SlicedDataDescription
    sliced_calculated_data = response.Web.SlicedCalculatedData
    for index, doc_sliced_data in enumerate(sliced_calculated_data):
        assert len(doc_sliced_data.SliceWithFeatures) == len(sliced_data_description.Slice2Features)
        concated_slices = _concat_slices(sliced_data_description.Slice2Features, doc_sliced_data.SliceWithFeatures)
        del doc_sliced_data.SliceWithFeatures[:]
        for concated_slice in concated_slices:
            doc_sliced_data.SliceWithFeatures.add().Features.extend(concated_slice)
    concated_descrs = _concat_description(sliced_data_description.Slice2Features)
    del sliced_data_description.Slice2Features[:]
    for concated_descr in concated_descrs:
        new_slice_to_feature = sliced_data_description.Slice2Features.add()
        new_slice_to_feature.SliceName = concated_descr[0]
        new_slice_to_feature.FeaturesIds.extend(concated_descr[1])


def _collect_feature_slices_diffs(first_data, second_data, data_description, compare_ctx=ComparePrecisionContext()):
    diffs = []
    for first_slice, second_slice, descr in zip(first_data.SliceWithFeatures, second_data.SliceWithFeatures, data_description.Slice2Features):
        if not compare_protobufs(first_slice, second_slice, compare_ctx.precision, compare_ctx.fields_to_ignore):
            diffs.append((first_slice, second_slice, descr))
    return diffs


def _collect_embeddings_diffs(first_data, second_data, data_description, compare_ctx=ComparePrecisionContext()):
    from kernel.dssm_applier.pylib.encode import decode_embedding
    diffs = []
    for first_embedding, second_embedding, embedding_id in zip(first_data.Embeddings, second_data.Embeddings, data_description.EmbeddingIds):
        first_decompressed, second_decompressed = decode_embedding(first_embedding), decode_embedding(second_embedding)
        single_value_diffs = []
        summary_error = 0
        for first_value, second_value in zip(first_decompressed, second_decompressed):
            summary_error += abs(first_value - second_value)
            if abs(first_value - second_value) > compare_ctx.precision:
                single_value_diffs.append(abs(first_value - second_value))
        if len(single_value_diffs) > compare_ctx.max_embeddings_diffs_count or summary_error > compare_ctx.embeddings_sum_error:
            diffs.append((embedding_id, single_value_diffs, summary_error))
    return diffs


def _collect_unpresented_features(first_descr, second_descr):
    first_features = set()
    second_features = set()
    for first_slice, second_slice in zip(first_descr.Slice2Features, second_descr.Slice2Features):
        first_features.update(first_slice.FeaturesIds)
        second_features.update(second_slice.FeaturesIds)
    got_unpresented_features = list(x for x in first_features if x not in second_features)
    expected_unpresented_features = list(x for x in second_features if x not in first_features)
    return (got_unpresented_features, expected_unpresented_features)


def check_responses_stripped(first, second, diff_file=None, compare_ctx=ComparePrecisionContext(), response_id=0):
    check_result = True
    unpresented_features = _collect_unpresented_features(first.Web.SlicedDataDescription, second.Web.SlicedDataDescription)
    if unpresented_features[0] or unpresented_features[1]:
        check_result = False
        print("ResponseId:", response_id, file=diff_file)
        print("Next features are not presented", end=': ', file=diff_file)
        print(unpresented_features[0], sep=' ', file=diff_file)
        print("Next features appeared in second_response", end=': ', file=diff_file)
        print(unpresented_features[1], sep=' ', file=diff_file)
    for first_data, second_data in zip(first.Web.SlicedCalculatedData, second.Web.SlicedCalculatedData):
        features_data = _collect_feature_slices_diffs(first_data, second_data, first.Web.SlicedDataDescription, compare_ctx)
        embeddings_data = _collect_embeddings_diffs(first_data, second_data, first.Web.SlicedDataDescription, compare_ctx)
        if embeddings_data or features_data:
            print("ResponseId:", response_id, file=diff_file)
            print("DocHandle:", first_data.DocHandle.Hash, file=diff_file)
        if features_data:
            check_result = False
            for first_slice, second_slice, descr in features_data:
                for index, (feature1, feature2) in enumerate(zip(first_slice.Features, second_slice.Features)):
                    if abs(feature1 - feature2) > compare_ctx.precision:
                        print("Id=%d | %f != %f | diff=%f" % (descr.FeaturesIds[index], feature1, feature2, abs(feature1 - feature2)), file=diff_file)
            print(file=diff_file)
        if embeddings_data:
            check_result = False
            for embedding_id, values, summary_error in embeddings_data:
                print("Id:", embedding_id, "- embeddings are not equal.", file=diff_file, end=' ')
                print("count=", len(values), file=diff_file, end=' ', sep='')
                print("sum_error=", summary_error, file=diff_file, end=' ', sep='')
                print("diffs=", values, file=diff_file, sep='')
            print(file=diff_file)
    return check_result


def check_responses(first_response, second_response, diff_file=None, compare_ctx=ComparePrecisionContext(), response_id=0):
    sortkey = lambda x: (x.DocHandle.Route, x.DocHandle.Hash)
    for r in (first_response, second_response):
        r.Web.SlicedCalculatedData.sort(key=sortkey)
        r.DocFPMResponse.SlicedCalculatedData.sort(key=sortkey)
        r.RapidClicksResponse.SlicedCalculatedData.sort(key=sortkey)
        r.CfgModelsResponse.SlicedCalculatedData.sort(key=sortkey)
        assert len(r.ModelsProxy.BigRtResponses) == len(r.ModelsProxy.BigRtResponsesTypes)
        indices = list(range(len(r.ModelsProxy.BigRtResponsesTypes)))
        indices.sort(key=lambda x: r.ModelsProxy.BigRtResponsesTypes[x])
        # types[indices[0]] < types[indices[1]] < ... < types[indices[-1]]
        sorted_bigrt_responses = []
        sorted_bigrt_responses_types = []
        for i in indices:
            sorted_bigrt_responses.append(r.ModelsProxy.BigRtResponses[i])
            sorted_bigrt_responses_types.append(r.ModelsProxy.BigRtResponsesTypes[i])
        assert all(sorted_bigrt_responses_types[i-1] < sorted_bigrt_responses_types[i] for i in range(1, len(sorted_bigrt_responses_types)))
        r.ModelsProxy.BigRtResponses[:] = sorted_bigrt_responses
        r.ModelsProxy.BigRtResponsesTypes[:] = sorted_bigrt_responses_types
        if not r.RapidClicksResponse.HasField("SlicedDataDescription"):
            r.RapidClicksResponse.SlicedDataDescription.SetInParent()
        _concat_sliced_data(r)
        r.ModelsProxy.RelCanonicalInfo.sort(key=sortkey)
        if 'Cached_CfgModels' in r.SearchProps:  # TODO: remove after next update of canonical answers
            del r.SearchProps['Cached_CfgModels']
    if not compare_protobufs(first_response, second_response, compare_ctx.precision, compare_ctx.fields_to_ignore) and diff_file is not None:
        print("ResponseId:", response_id, file=diff_file)
        print("--- got ---", file=diff_file)
        print(MessageToString(second_response, use_short_repeated_primitives=True), file=diff_file)
        print("--- expected ---", file=diff_file)
        print(MessageToString(first_response, use_short_repeated_primitives=True), file=diff_file)
        return False
    return True
