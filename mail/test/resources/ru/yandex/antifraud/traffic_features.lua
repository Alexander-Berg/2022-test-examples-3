local Base64 = luajava.bindClass("java.util.Base64");
local FingerprintV1 = luajava.bindClass("fingerprint.v1.FingerprintV1");
local Hex = luajava.bindClass("org.bouncycastle.util.encoders.Hex");
local Integer = luajava.bindClass("java.lang.Integer");

local SYN_TCP_FEATURES = {
    "syn_mss",
    "syn_opt_eol_pad",
    "syn_quirk_df",
    "syn_quirk_ecn",
    "syn_quirk_flow",
    "syn_quirk_nz_ack",
    "syn_quirk_nz_id",
    "syn_quirk_nz_mbz",
    "syn_quirk_nz_urg",
    "syn_quirk_opt_bad",
    "syn_quirk_opt_eol_nz",
    "syn_quirk_opt_exws",
    "syn_quirk_opt_nz_ts2",
    "syn_quirk_opt_zero_ts1",
    "syn_quirk_push",
    "syn_quirk_urg",
    "syn_quirk_zero_ack",
    "syn_quirk_zero_id",
    "syn_quirk_zero_seq",
    "syn_win",
    "syn_win_scale",
    "syn_win_type",
    "no_ts_opt_percent"
};

local SSL_CIPHERSUITES_FEATURES = {};
for i, cs in pairs({ "0001", "0002", "0003", "0004", "0005", "0006",
                     "0007", "0008", "0009", "000a", "000c", "000d",
                     "0010", "0011", "0012", "0013", "0014", "0015",
                     "0016", "0018", "001a", "001b", "001c", "001e",
                     "001f", "0020", "0022", "0023", "0024", "002f",
                     "0030", "0031", "0032", "0033", "0034", "0035",
                     "0036", "0037", "0038", "0039", "003a", "003b",
                     "003c", "003d", "003e", "003f", "0040", "0041",
                     "0042", "0043", "0044", "0045", "0060", "0067",
                     "0068", "0069", "006a", "006b", "006c", "006d",
                     "0081", "0084", "0085", "0086", "0087", "0088",
                     "0094", "0096", "0097", "0098", "0099", "009a",
                     "009c", "009d", "009e", "009f", "00a0", "00a1",
                     "00a2", "00a3", "00a4", "00a5", "00a6", "00a7",
                     "00ba", "00bd", "00be", "00c0", "00c3", "00c4",
                     "00ff", "05ec", "0a0a", "1301", "1302", "1303",
                     "1304", "1a1a", "201a", "268d", "2986", "2a2a",
                     "3a3a", "4a4a", "5600", "5a5a", "5e3f", "6a6a",
                     "7a40", "7a7a", "8221", "8a8a", "901d", "9a9a",
                     "aaaa", "b0ca", "baba", "c001", "c002", "c003",
                     "c004", "c005", "c006", "c007", "c008", "c009",
                     "c00a", "c00b", "c00c", "c00d", "c00e", "c00f",
                     "c010", "c011", "c012", "c013", "c014", "c015",
                     "c016", "c017", "c018", "c019", "c01b", "c01c",
                     "c01e", "c01f", "c021", "c022", "c023", "c024",
                     "c025", "c026", "c027", "c028", "c029", "c02a",
                     "c02b", "c02c", "c02d", "c02e", "c02f", "c030",
                     "c031", "c032", "c050", "c051", "c052", "c053",
                     "c056", "c057", "c05c", "c05d", "c060", "c061",
                     "c072", "c073", "c076", "c077", "c09c", "c09d",
                     "c09e", "c09f", "c0a0", "c0a1", "c0a2", "c0a3",
                     "c0ac", "c0ad", "c0ae", "c0af", "caca", "cca8",
                     "cca9", "ccaa", "d0c2", "dada", "eaea", "fafa",
                     "ff85" }) do
    SSL_CIPHERSUITES_FEATURES[i] = "ssl_ciphersuites_" .. cs;
end

local SSL_EXTENSIONS_FEATURES = {};
for i, ext in pairs({
    "0000", "0001", "0005", "000a", "000b", "000d",
    "000f", "0010", "0011", "0012", "0015", "0016",
    "0017", "001b", "001c", "0023", "002b", "002d",
    "0031", "0032", "0033", "00ff", "03cb", "0a0a",
    "0c29", "0dc9", "1195", "1301", "1a1a", "1db4",
    "2a2a", "3374", "3a3a", "3a7c", "4a4a", "5a5a",
    "61cb", "69ab", "6a6a", "7550", "7a7a", "8a8a",
    "9317", "9a9a", "aaaa", "baba", "c99b", "caca",
    "d0c2", "dada", "eaea", "fafa", "ff01" }) do
    SSL_EXTENSIONS_FEATURES[i] = "ssl_extensions_" .. ext
end

local SUSPICIOUS_SSL_FEATURES = {
    "suspicious_ssl_client_version",
    "suspicious_ssl_extension_size",
    "suspicious_ssl_compression_size",
    "suspicious_ssl_record_size",
    "suspicious_ssl_all_extensions_size",
    "suspicious_ssl_handshake_size"
};

local CLIENT_VERSIONS = { "0303", "0301", "0302", "not_set", "0304" };

local CLIENT_VERSIONS_FEATURES = {};
for i, ver in pairs(CLIENT_VERSIONS) do
    CLIENT_VERSIONS_FEATURES[i] = "ssl_client_version_" .. ver;
end

local PROTOCOL_VERSIONS_FEATURES = {};
for i, ver in pairs({ "0301", "0303", "not_set", "0300", "0302" }) do
    PROTOCOL_VERSIONS_FEATURES[i] = "ssl_protocol_version_" .. ver;
end

function make_features(raw_traffic_fp)
    local traffic_fp = FingerprintV1.Fingerprint:parseFrom(
        Base64:getDecoder():decode(raw_traffic_fp)
    );

    local features = {};

    local binaryParams = traffic_fp:getBinaryParams();

    for _, features_list in pairs({
        SYN_TCP_FEATURES,
        SSL_CIPHERSUITES_FEATURES,
        SSL_EXTENSIONS_FEATURES,
        SSL_EXTENSIONS_FEATURES,
        SUSPICIOUS_SSL_FEATURES,
        CLIENT_VERSIONS_FEATURES,
        PROTOCOL_VERSIONS_FEATURES }) do
        for _, feature in pairs(features_list) do
            features[feature] = -1.;
        end
    end

    if binaryParams:getSynMss() ~= 0 then
        features["syn_mss"] = binaryParams:getSynMss();
    end

    if binaryParams:getSynOptEolPad() ~= 0 then
        features["syn_opt_eol_pad"] = binaryParams:getSynOptEolPad();
    end

    if binaryParams:getSynQuirkDf() then
        features["syn_quirk_df"] = 1;
    end

    if binaryParams:getSynQuirkFlow() then
        features["syn_quirk_flow"] = 1;
    end

    if binaryParams:getSynQuirkNzAck() then
        features["syn_quirk_nz_ack"] = 1;
    end

    if binaryParams:getSynQuirkNzId() then
        features["syn_quirk_nz_id"] = 1;
    end

    if binaryParams:getSynQuirkNzMbz() then
        features["syn_quirk_nz_mbz"] = 1;
    end

    if binaryParams:getSynQuirkNzUrg() then
        features["syn_quirk_nz_urg"] = 1;
    end

    if binaryParams:getSynQuirkOptBad() then
        features["syn_quirk_opt_bad"] = 1;
    end

    if binaryParams:getSynQuirkOptEolNz() then
        features["syn_quirk_opt_eol_nz"] = 1;
    end

    if binaryParams:getSynQuirkOptExws() then
        features["syn_quirk_opt_exws"] = 1;
    end

    if binaryParams:getSynQuirkOptNzTs2() then
        features["syn_quirk_opt_nz_ts2"] = 1;
    end

    if binaryParams:getSynQuirkOptZeroTs1() then
        features["syn_quirk_opt_zero_ts1"] = 1;
    end

    if binaryParams:getSynQuirkPush() then
        features["syn_quirk_push"] = 1;
    end

    if binaryParams:getSynQuirkUrg() then
        features["syn_quirk_urg"] = 1;
    end

    if binaryParams:getSynQuirkZeroAck() then
        features["syn_quirk_zero_ack"] = 1;
    end

    if binaryParams:getSynQuirkZeroId() then
        features["syn_quirk_zero_id"] = 1;
    end

    if binaryParams:getSynQuirkZeroSeq() then
        features["syn_quirk_zero_seq"] = 1;
    end

    if binaryParams:getSynWin() ~= 0 then
        features["syn_win"] = binaryParams:getSynWin();
    end

    if binaryParams:getSynWinScale() ~= 0 then
        features["syn_win_scale"] = binaryParams:getSynWinScale();
    end

    if binaryParams:getSynWinType() ~= 0 then
        features["syn_win_type"] = binaryParams:getSynWinType();
    end

    if binaryParams:getNoTsOptPercent() > 0 then
        features["no_ts_opt_percent"] = binaryParams:getNoTsOptPercent();
    end

    local tls_params = binaryParams:getTlsParams();

    for i = 0, tls_params:getSslCiphersuitesCount() - 1 do
        local cipher_suite = tls_params:getSslCiphersuites(i);
        local feature_name = "ssl_ciphersuites_" .. Hex:toHexString(cipher_suite:toByteArray());
        features[feature_name] = 1. / (1 + i);
    end

    local sslExtensionsList = tls_params:getSslExtensionsList();
    for i = 0, sslExtensionsList:size() - 1 do
        local tls_extension = sslExtensionsList:get(i);
        local feature_name = "ssl_extensions_" .. Hex:toHexString(tls_extension:getKey():toByteArray());
        features[feature_name] = 1;
    end

    if tls_params:getSuspiciousSslClientVersion() then
        features["suspicious_ssl_client_version"] = 1;
    end

    if tls_params:getSuspiciousSslExtensionSize() then
        features["suspicious_ssl_extension_size"] = 1;
    end

    if tls_params:getSuspiciousSslCompressionSize() then
        features["suspicious_ssl_compression_size"] = 1;
    end

    if tls_params:getSuspiciousSslRecordSize() then
        features["suspicious_ssl_record_size"] = 1;
    end

    if tls_params:getSuspiciousSslAllExtensionsSize() then
        features["suspicious_ssl_all_extensions_size"] = 1;
    end

    if tls_params:getSuspiciousSslHandshakeSize() then
        features["suspicious_ssl_handshake_size"] = 1;
    end

    if tls_params:getSslClientVersion() ~= 0 then
        for _, version in pairs(CLIENT_VERSIONS) do
            local hexed = Integer:toHexString(tls_params:getSslClientVersion());
            if version == hexed then
                features["ssl_client_version_" .. hexed] = 1;
            end
        end
    else
        features["ssl_client_version_not_set"] = 1;
    end

    if tls_params:getSslProtocolVersion() ~= 0 then
        for _, version in pairs(CLIENT_VERSIONS) do
            local hexed = Integer:toHexString(tls_params:getSslProtocolVersion());
            if version == hexed then
                features["ssl_protocol_version_" .. hexed] = 1;
            end
        end
    else
        features["ssl_protocol_version_not_set"] = 1;
    end

    return features;
end

return {
    ["make_features"] = make_features
};
