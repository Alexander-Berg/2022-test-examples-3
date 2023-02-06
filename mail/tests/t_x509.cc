#include <yxiva/core/x509.h>
#include <catch.hpp>

using namespace yxiva::x509;

extern const std::string valid_cert_pem;
extern const std::string valid_chain_pem;
extern const std::vector<char> valid_chain_p12;
extern const std::string valid_private_key;
const std::string p12_password = "password";

TEST_CASE("x509/certificate/extracts_certificate_fields")
{
    X509* cert_ptr;
    auto result = parse_pem(valid_cert_pem.data(), valid_cert_pem.size(), cert_ptr);
    certificate cert(cert_ptr);

    REQUIRE(result);

    std::string text;

    // Issuer: C=US, O=Apple Inc., OU=Apple Worldwide Developer Relations, CN=Apple Worldwide
    // Developer Relations Certification Authority
    {
        auto result = cert.issuer_text(attribute_type::country_name, text);
        REQUIRE(result);
        REQUIRE(text == "US");
    }

    {
        auto result = cert.issuer_text(attribute_type::organization_name, text);
        REQUIRE(result);
        REQUIRE(text == "Apple Inc.");
    }

    {
        auto result = cert.issuer_text(attribute_type::organizational_unit_name, text);
        REQUIRE(result);
        REQUIRE(text == "Apple Worldwide Developer Relations");
    }

    {
        auto result = cert.issuer_text(attribute_type::common_name, text);
        REQUIRE(result);
        REQUIRE(text == "Apple Worldwide Developer Relations Certification Authority");
    }

    // Subject: UID=ru.yandex.blue.market.inhouse.debug, CN=Apple Development IOS Push Services:
    // ru.yandex.blue.market.inhouse.debug, OU=EK7Z26L6D4, C=US
    {
        auto result = cert.subject_text(attribute_type::user_id, text);
        REQUIRE(result);
        REQUIRE(text == "ru.yandex.blue.market.inhouse.debug");
    }

    {
        auto result = cert.subject_text(attribute_type::common_name, text);
        REQUIRE(result);
        REQUIRE(text == "Apple Development IOS Push Services: ru.yandex.blue.market.inhouse.debug");
    }

    {
        auto result = cert.subject_text(attribute_type::organizational_unit_name, text);
        REQUIRE(result);
        REQUIRE(text == "EK7Z26L6D4");
    }

    {
        auto result = cert.subject_text(attribute_type::country_name, text);
        REQUIRE(result);
        REQUIRE(text == "US");
    }
}

TEST_CASE("x509/contains_pem/returns_true_on_pem")
{
    REQUIRE(contains_pem(valid_cert_pem.data(), valid_cert_pem.size()));
}

TEST_CASE("x509/contains_pem/returns_false_on_not_pem")
{
    REQUIRE(!contains_pem(valid_chain_p12.data(), valid_chain_p12.size()));
}

TEST_CASE("x509/parse_pem/parses_valid_certificate")
{
    X509* cert_ptr = nullptr;
    REQUIRE(parse_pem(valid_cert_pem.data(), valid_cert_pem.size(), cert_ptr));
    REQUIRE(cert_ptr != nullptr);
    X509_free(cert_ptr);
}

TEST_CASE("x509/parse_pem/returns_error_on_invalid_certificate")
{
    const std::string invalid_cert = "not_a_certificate";

    X509* cert_ptr;
    auto result = parse_pem(invalid_cert.data(), invalid_cert.size(), cert_ptr);
    REQUIRE(!result);
}

TEST_CASE("x509/parse_p12/parses_valid_certificate_chain")
{
    certificate_chain certs;
    REQUIRE(parse_p12(valid_chain_p12.data(), valid_chain_p12.size(), p12_password, certs));
    REQUIRE(certs.size() == 2);
}

TEST_CASE("x509/parse_p12/returns_error_on_wrong_password")
{
    certificate_chain certs;
    auto result =
        parse_p12(valid_chain_p12.data(), valid_chain_p12.size(), "wrong" + p12_password, certs);
    REQUIRE(!result);
}

TEST_CASE("x509/parse_p12/returns_error_on_invalid_certificate")
{
    const std::string invalid_cert = "not_a_certificate";

    certificate_chain certs;
    auto result = parse_p12(valid_cert_pem.data(), valid_cert_pem.size(), p12_password, certs);
    REQUIRE(!result);
}

TEST_CASE("x509/parse_private_key/parses_valid_private_key")
{
    EVP_PKEY* private_key_ptr = nullptr;
    auto result =
        parse_private_key(valid_private_key.data(), valid_private_key.size(), private_key_ptr);
    std::cout << result.error_reason << std::endl;
    REQUIRE(result);
    REQUIRE(private_key_ptr != nullptr);
    EVP_PKEY_free(private_key_ptr);
}

TEST_CASE("x509/parse_private_key/returns_error_on_invalid_private_key")
{
    const std::string invalid_private_key = "not_a_private_key";

    EVP_PKEY* private_key_ptr;
    auto result =
        parse_private_key(invalid_private_key.data(), invalid_private_key.size(), private_key_ptr);
    REQUIRE(!result);
}

TEST_CASE("x509/write_pem/writes_valid_certificate_chain")
{
    certificate_chain certs;
    std::string certs_in_pem;
    parse_p12(valid_chain_p12.data(), valid_chain_p12.size(), p12_password, certs);
    REQUIRE(write_pem(certs, certs_in_pem));
    REQUIRE(certs_in_pem == valid_chain_pem);
}

const std::string valid_cert_pem = R"raw_cert(-----BEGIN CERTIFICATE-----
MIIFqzCCBJOgAwIBAgIIdc5Mi0IQSxYwDQYJKoZIhvcNAQEFBQAwgZYxCzAJBgNV
BAYTAlVTMRMwEQYDVQQKDApBcHBsZSBJbmMuMSwwKgYDVQQLDCNBcHBsZSBXb3Js
ZHdpZGUgRGV2ZWxvcGVyIFJlbGF0aW9uczFEMEIGA1UEAww7QXBwbGUgV29ybGR3
aWRlIERldmVsb3BlciBSZWxhdGlvbnMgQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkw
HhcNMTgwNTAzMTAwNjA2WhcNMTkwNTAzMTAwNjA2WjCBqjEzMDEGCgmSJomT8ixk
AQEMI3J1LnlhbmRleC5ibHVlLm1hcmtldC5pbmhvdXNlLmRlYnVnMVEwTwYDVQQD
DEhBcHBsZSBEZXZlbG9wbWVudCBJT1MgUHVzaCBTZXJ2aWNlczogcnUueWFuZGV4
LmJsdWUubWFya2V0LmluaG91c2UuZGVidWcxEzARBgNVBAsMCkVLN1oyNkw2RDQx
CzAJBgNVBAYTAlVTMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwh/S
yhDxDNtfH2eZU5ZLljteGSlK0O0bJaikIrZVsHSGPvASZ694dM3SxGOdEcE999WS
CyOoMr5lpwWnDmrCo5j9Iif5nUpXYeGFK1S0pJ3OQAO3JvBkWHL7aQUjhDGgoowu
ElWVqfSDRD+rQyDFU8Qa8/6uBMDwWTpHOM6KFH6PKKy5OP58UCmzAJe5Mf1rDd78
KkMDbvxjBqL5X9OyQVuS8Mb0aIfNpGQ5Yl7wAOtTTdJbHnZ+JsTNTyjq4R6RzxXr
xjW82rVmmy0K5liPLahQeFDSlgZemZUNRNmhblEUHZEQ6bKK9UCvqTZQR7zMzSTQ
9/o5V/UXWJbeNBny+wIDAQABo4IB5TCCAeEwCQYDVR0TBAIwADAfBgNVHSMEGDAW
gBSIJxcJqbYYYIvs67r2R1nFUlSjtzCCAQ8GA1UdIASCAQYwggECMIH/BgkqhkiG
92NkBQEwgfEwgcMGCCsGAQUFBwICMIG2DIGzUmVsaWFuY2Ugb24gdGhpcyBjZXJ0
aWZpY2F0ZSBieSBhbnkgcGFydHkgYXNzdW1lcyBhY2NlcHRhbmNlIG9mIHRoZSB0
aGVuIGFwcGxpY2FibGUgc3RhbmRhcmQgdGVybXMgYW5kIGNvbmRpdGlvbnMgb2Yg
dXNlLCBjZXJ0aWZpY2F0ZSBwb2xpY3kgYW5kIGNlcnRpZmljYXRpb24gcHJhY3Rp
Y2Ugc3RhdGVtZW50cy4wKQYIKwYBBQUHAgEWHWh0dHA6Ly93d3cuYXBwbGUuY29t
L2FwcGxlY2EvMBMGA1UdJQQMMAoGCCsGAQUFBwMCME0GA1UdHwRGMEQwQqBAoD6G
PGh0dHA6Ly9kZXZlbG9wZXIuYXBwbGUuY29tL2NlcnRpZmljYXRpb25hdXRob3Jp
dHkvd3dkcmNhLmNybDAdBgNVHQ4EFgQUSj4tUZAHvX1HRmsKw2FWOlJqJKIwCwYD
VR0PBAQDAgeAMBAGCiqGSIb3Y2QGAwEEAgUAMA0GCSqGSIb3DQEBBQUAA4IBAQBi
+jQpbLvNJII7zOz908a0YXSRp1prjLBG946k56QyfrHAHdBl/pTStpx5GHTtp74k
wa1pl0BrNh3hi7W6kFwU+NamtpqfK1NCfvbD0SrZAqn0tg8wQxP+c2LhR/UPsJ33
TIMtrtt3H/PVso0O1gA/qMgjQVZGXg2iAd1gxxBFRpjMq0bjVwX4M6U+IBpNdxE6
JTeLbRF3xGPz2nvA72w6n+g2BrxhAau2BEfWDOmSzN1zK8O+wfLCU5KZPOXH5BUL
birjoRD3u7Hc6jeUKb2dqZdKiTny+/R3PKjQIxi28lt2+S73iEaSibGTb60V6XUQ
SO8n8hcoD2AGMszZaA7G
-----END CERTIFICATE-----)raw_cert";

// Certs below are signed by a demoCA, no sensitive data here.
const std::string valid_chain_pem = R"raw_cert(-----BEGIN PRIVATE KEY-----
MIIEuwIBADANBgkqhkiG9w0BAQEFAASCBKUwggShAgEAAoIBAQDE6lHg/+cv0oXK
QZmBjQmMjmcNFkwOIidWv0DsJwkEB+p9MecKolZLekZOVhhoiR83sokJ/cu8s1dU
d7flrOD+VeD5Jo3yBAo+ihypQ6vhfmuOmpT/FraIoLvOomprQdrAFbjgQhhyDcJk
uHEjl4xkPY2NSuPPoweY+MD1w3RUsf9Wu8IOAGzdBksYpfdcYedD95v2k1vyj/sX
PUhr9XgDRTAyWEftyykjGuWS8DiXYWx4apsTab0+Mrf6o9/3rgBLkm3lfjNWzSNL
PuU9RbAhFLxZHepjw4ffaWJnTnEUeobZaKgU0vw1yOOP+Z+7f3860sCixkxVdY1Z
iIIWr9fJAgMBAAECggEANt4npG8JpvydBOdUc5Gt6HlBY5cj6AhCN9ygpWyXCYV/
JBVEkGT9cJiIgIli0bXreeSIZL1QCKtcGRAmsJuEMboSLro8cQQJKrXnGI7flLxa
EekmvRFCGT3YNtJwnqRB5KXo1qv0Dcuhr9joxYcTulagW49TfOarVkdx1Mj6YCKe
/yZ2du21NrXo5kiP+gpDui9Dq5qo3WYTBILFKDVIMVRMV6pac0y/c4BCO8PPS11I
fVscTgTCXN98k1A2nBwJqxdR9NLs72ZXwIKlZqqLlF3nM2jWDMmjYVJYUNaQedNq
cgkVVY/CSlpcuHmJ5C2TXeUexgiqJ7Ev+P1DgI9sEQKBgQD43DrwqjmAf5EFzwEv
QMN4nr1xvTEoKadfQ01Y8o3VXqVazxgOl+j9nmpW3K8aHTySD4w7Y7JTgkQM2JIZ
AFep0/5j3cQooRSLOC+ScYTuiJ4PCnpL1G/IEekTpfMYETTfxOJlAB2EVikNz19Q
LHdom4jBgokaLZRAtnHwAEGHtQKBgQDKkJOb1PavEYBvaXI5x3IbiOi3heFgJ2lE
zdeMEPGIujwUTiSBwyFe80oKZpkderXsW+SIaN0h9lZcQFNGWhBJxzTAcVVG6qqv
Y9hww5g873yHaNqjJjtBeoNMe05KqnpDEAuFXhGWvd8hJga204WJSmXCPU7mMjUN
oijbxg+0RQKBgDYYeV9zQUijcjzXAKTq6RLBPuEIhTT7yPw2PwwEvbwR9NPGjK4v
a2AmHWuAUTfV/yKo6ozGYS8x8+Tu9uMkZenuwS+oN0m1qieRMRYeQw0u+Vy/pFHz
fdD5w7aXrj4fD2VNAQF/gusPZ6Eu4MMmx1Yf6RxXacIdAksG3cGT7Kq5AoGBALcf
ksIK+eP0z4Zo7mG7o7WuxKu8ta3Gk00nRZiTbLetnUjCCBDjRdTf3myeqgdTewmt
XWHQYzEj0JjfcikknJsGIvNiwbQ1y455lh8DRCq6WnrT2OwrUOB2ECLhSjsH+TWN
WMz96Kq9Be2iwekgOhWOk4XRBPXoJ7wmgAeG+4V9An95R09+N/NIhPPScr1hlZY7
EbStDclsiTVpyBmo14rLXvfbHBzQ3cLirfJaTFm1F1SiAuKKbBc5o0tWTWXhWMCN
NTMyix5ZfwH+J3V6uVSSrzIrvvaPWoMueQn4iYm7dSytP6AaosPBpux8OcDZtetW
c5znrBdZqmZ59f1pbuMU
-----END PRIVATE KEY-----
-----BEGIN CERTIFICATE-----
MIIDsjCCApqgAwIBAgIJAJjcBfdxiW69MA0GCSqGSIb3DQEBCwUAMFExCzAJBgNV
BAYTAlVTMQswCQYDVQQIDAI1MjENMAsGA1UECgwEeGl2YTEXMBUGA1UECwwOeGl2
YWNvcmVfdXRlc3QxDTALBgNVBAMMBHJvb3QwHhcNMTcwNjI4MTEwMzQzWhcNMjcw
NjI2MTEwMzQzWjBjMQswCQYDVQQGEwJVUzELMAkGA1UECAwCNTIxEDAOBgNVBAcM
B05vd2hlcmUxDTALBgNVBAoMBHhpdmExFzAVBgNVBAsMDnhpdmFjb3JlX3V0ZXN0
MQ0wCwYDVQQDDAR0YWlsMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA
xOpR4P/nL9KFykGZgY0JjI5nDRZMDiInVr9A7CcJBAfqfTHnCqJWS3pGTlYYaIkf
N7KJCf3LvLNXVHe35azg/lXg+SaN8gQKPoocqUOr4X5rjpqU/xa2iKC7zqJqa0Ha
wBW44EIYcg3CZLhxI5eMZD2NjUrjz6MHmPjA9cN0VLH/VrvCDgBs3QZLGKX3XGHn
Q/eb9pNb8o/7Fz1Ia/V4A0UwMlhH7cspIxrlkvA4l2FseGqbE2m9PjK3+qPf964A
S5Jt5X4zVs0jSz7lPUWwIRS8WR3qY8OH32liZ05xFHqG2WioFNL8Ncjjj/mfu39/
OtLAosZMVXWNWYiCFq/XyQIDAQABo3sweTAJBgNVHRMEAjAAMCwGCWCGSAGG+EIB
DQQfFh1PcGVuU1NMIEdlbmVyYXRlZCBDZXJ0aWZpY2F0ZTAdBgNVHQ4EFgQUuRqH
vfU3exJHpxqB96nwj5dDST4wHwYDVR0jBBgwFoAU2LWwKThT0lu/oDI2SMDReah2
vWowDQYJKoZIhvcNAQELBQADggEBACluC1h6rDD1OFLqwxuoQqPZejSChffxU5+s
rRGVCqXgrnLKHLjDVHkrZ4WtSezSgzsBA2buxSWUCtmjqR010slE/YMDd4XYtWXQ
0oOCZ18ovr6/DLViwHTUbRkxBBkGFSdSQiBZsdatc31Hz8vJ+kay0nMmamrb5Y1E
pwO40c0NMzfIfP2JH/btt3vcJyZA6R1DCKMO1MfhCwuTKF31YXAZF6rZ+mf3//kG
Dx96Vzp59zzUIYxyyyY1KrKFJcC9ltyQzJHLGj17CsjyMcg48mczoQE0Iww8tBu3
R6354F0F34oaFrKErbYgKZ2NgvevR6CI15xhdiNdycc3TlzB9XE=
-----END CERTIFICATE-----
-----BEGIN CERTIFICATE-----
MIIDdTCCAl2gAwIBAgIJAJjcBfdxiW68MA0GCSqGSIb3DQEBCwUAMFExCzAJBgNV
BAYTAlVTMQswCQYDVQQIDAI1MjENMAsGA1UECgwEeGl2YTEXMBUGA1UECwwOeGl2
YWNvcmVfdXRlc3QxDTALBgNVBAMMBHJvb3QwHhcNMTcwNjI4MTEwMjIzWhcNMjAw
NjI3MTEwMjIzWjBRMQswCQYDVQQGEwJVUzELMAkGA1UECAwCNTIxDTALBgNVBAoM
BHhpdmExFzAVBgNVBAsMDnhpdmFjb3JlX3V0ZXN0MQ0wCwYDVQQDDARyb290MIIB
IjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzuLFvuVevdmA0xJLN77n00Yt
00gV9zHQQQRY7r9qtAz5OM4MzG3/BKqOoFgFU05h3dbes9euJLrV3WNuRPHqBqkz
E7l1azbItS1LycL7TDBg1tJ/roBIneWtc1WYIfT0LUlx4qW5FSkrRGYBNtaziB1g
yFHtjgUeQjZ7nM2aFFaVMfBMqy+jfVEMV3gOER2mHfi5spU6OHpMsxFYZ0e5Qlrb
YnHwW1j5VCaoMbRzcUhi1M4i413jFjGBLlHzAirPRbR3jSGWV+19YO/sd1hrZCNB
95CMlk8pRjwMjxIb4YxUJZTIvx5aNZwsY2iiQ6z0ALYFP+RNcL8TZ3JbfjOPxQID
AQABo1AwTjAdBgNVHQ4EFgQU2LWwKThT0lu/oDI2SMDReah2vWowHwYDVR0jBBgw
FoAU2LWwKThT0lu/oDI2SMDReah2vWowDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0B
AQsFAAOCAQEAC/8e7Jn+SChFpMfvme2FM5h7Zjqp/vWatYK/sTQwhiK77K7rDzXa
ghxMTmN38GZvnx85J2dX3fn4RAghBcMzg1dhXijvn+MJqhtho3hwa4Lz0QPHw7SO
5LYQmi37MFzhJ6/1dzH9PUP4OMAQ/meEHgWEecD1BaM0fWmDxdGPP/GKoWVq7lkQ
JLONnEahVg5jPw3LxTqDs/s3KlOVjU9V8ogGF0YpDn3vL2TxPaA3gcCPs7yZt3w5
HxiWfg1f4mJLcQOdTRMLxbufkF+vrEFuyu+ajnvV34Qi+rMMy9PT2UpNRuBk0DRx
5+YKOrByegeB1aJ2XEAUnIz5a8/8PoDs5Q==
-----END CERTIFICATE-----
)raw_cert";

unsigned char chain_p12[] = {
    0x30, 0x82, 0x0d, 0xc2, 0x02, 0x01, 0x03, 0x30, 0x82, 0x0d, 0x88, 0x06, 0x09, 0x2a, 0x86, 0x48,
    0x86, 0xf7, 0x0d, 0x01, 0x07, 0x01, 0xa0, 0x82, 0x0d, 0x79, 0x04, 0x82, 0x0d, 0x75, 0x30, 0x82,
    0x0d, 0x71, 0x30, 0x82, 0x08, 0x17, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x07,
    0x06, 0xa0, 0x82, 0x08, 0x08, 0x30, 0x82, 0x08, 0x04, 0x02, 0x01, 0x00, 0x30, 0x82, 0x07, 0xfd,
    0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x07, 0x01, 0x30, 0x1c, 0x06, 0x0a, 0x2a,
    0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x0c, 0x01, 0x06, 0x30, 0x0e, 0x04, 0x08, 0xe9, 0xee, 0xdc,
    0x1e, 0x73, 0xc8, 0x89, 0xa3, 0x02, 0x02, 0x08, 0x00, 0x80, 0x82, 0x07, 0xd0, 0x56, 0x27, 0x23,
    0x68, 0x66, 0x07, 0x29, 0xa7, 0x54, 0xba, 0x80, 0xae, 0x6f, 0x96, 0x24, 0xff, 0x6a, 0xbf, 0xc7,
    0x95, 0xdd, 0xc1, 0x7d, 0x99, 0xfa, 0xc9, 0x1a, 0x6e, 0x5e, 0xc3, 0x2d, 0xf4, 0x44, 0x37, 0xc8,
    0xab, 0x0a, 0xc5, 0x99, 0x5e, 0x0a, 0xdc, 0xf6, 0x7c, 0xa4, 0xf7, 0x53, 0x83, 0xdb, 0x88, 0x7c,
    0x63, 0xf7, 0xab, 0x9e, 0x71, 0x37, 0xab, 0x92, 0xbf, 0xc0, 0x52, 0xf5, 0xef, 0xc2, 0x16, 0x80,
    0xe1, 0xc4, 0x20, 0x3e, 0x31, 0x6a, 0xff, 0xdd, 0xc2, 0xb2, 0x74, 0x9e, 0x10, 0x64, 0x09, 0x49,
    0x26, 0x23, 0x32, 0xe5, 0xed, 0xba, 0xc3, 0x8f, 0xc1, 0xa3, 0x5a, 0x0f, 0x29, 0x9a, 0xd5, 0xee,
    0xbd, 0x8e, 0x8c, 0x3e, 0x9e, 0x1d, 0x6b, 0x1d, 0x0f, 0x35, 0xe8, 0x6c, 0xc8, 0x65, 0x7a, 0xa8,
    0x2b, 0x78, 0xc1, 0x01, 0xde, 0x80, 0x3f, 0x08, 0x0c, 0xab, 0x9a, 0x10, 0xf5, 0xec, 0x20, 0x89,
    0x11, 0xf6, 0x8d, 0x0c, 0x67, 0x4b, 0x25, 0xf9, 0xa6, 0xeb, 0x63, 0x2e, 0xc3, 0xe8, 0x49, 0x2f,
    0x17, 0x2f, 0xf3, 0xb1, 0x4e, 0x81, 0xe9, 0x24, 0x64, 0xe8, 0x8e, 0xa2, 0x95, 0xc3, 0x9d, 0xb1,
    0xe0, 0x7a, 0xd5, 0x0c, 0x3d, 0x25, 0x89, 0x99, 0x27, 0xb6, 0x11, 0xfa, 0x38, 0x66, 0x84, 0x34,
    0x78, 0x63, 0x84, 0xcf, 0x1c, 0xd7, 0x31, 0x5f, 0xe5, 0xca, 0x86, 0xf0, 0x92, 0xfd, 0x18, 0x19,
    0xa7, 0xb2, 0xc9, 0x44, 0x9d, 0x94, 0x31, 0xb3, 0xc1, 0xc7, 0x92, 0x5c, 0xbb, 0x88, 0x81, 0xe3,
    0xee, 0x64, 0x6e, 0x0a, 0x7b, 0x8d, 0x8c, 0x42, 0x80, 0x1d, 0x35, 0xb1, 0x67, 0x94, 0x2a, 0x08,
    0x4d, 0xbe, 0x76, 0x93, 0x55, 0x18, 0xe7, 0xe3, 0x66, 0xdc, 0xe2, 0x92, 0xb1, 0x26, 0x23, 0x97,
    0xa8, 0xa6, 0x4a, 0xb9, 0x62, 0xa8, 0x2f, 0x14, 0x03, 0xff, 0x96, 0xb9, 0x88, 0x1c, 0x04, 0xfc,
    0x74, 0xca, 0xb5, 0x1c, 0x64, 0xb2, 0xa8, 0x3c, 0x23, 0xfa, 0xeb, 0xbb, 0x7a, 0x71, 0x77, 0x24,
    0x3a, 0x3c, 0x81, 0x6c, 0x74, 0x93, 0xcd, 0x75, 0xbe, 0x18, 0x87, 0x53, 0x24, 0xe1, 0x0d, 0xc8,
    0x73, 0x6b, 0x2c, 0xa8, 0xcc, 0xa3, 0xde, 0xae, 0x8f, 0xcc, 0xaa, 0x49, 0x4f, 0x74, 0x72, 0xbc,
    0xdd, 0x6e, 0x97, 0x1c, 0xf3, 0x6c, 0xd3, 0x4d, 0x51, 0x9d, 0xb2, 0x5c, 0xd9, 0x4f, 0x0a, 0xf0,
    0x06, 0xfa, 0xbd, 0xa6, 0x43, 0x94, 0xe9, 0x5d, 0x54, 0xe9, 0xed, 0x63, 0x71, 0x3a, 0x13, 0x2c,
    0x54, 0xd8, 0x02, 0x91, 0x0b, 0x5f, 0x0f, 0x80, 0xfd, 0x32, 0x53, 0xc8, 0x4b, 0x22, 0x40, 0xaf,
    0xac, 0x9c, 0x30, 0x90, 0xaf, 0x4b, 0xf4, 0xe0, 0xf4, 0xa2, 0x7b, 0xee, 0xab, 0x13, 0x6d, 0x34,
    0xb8, 0xc7, 0x1c, 0x8a, 0xb4, 0xd2, 0x4f, 0x3a, 0x11, 0xa4, 0x1a, 0x9b, 0xe0, 0x8a, 0x57, 0xe3,
    0x4f, 0xf7, 0x71, 0xf2, 0x3f, 0xe5, 0x7d, 0x8b, 0x68, 0x9a, 0xac, 0x1f, 0xf4, 0x9a, 0x0e, 0xb1,
    0xf5, 0xc9, 0x48, 0x1d, 0xf4, 0x39, 0xb4, 0xe3, 0xd6, 0x43, 0x34, 0xec, 0x55, 0xd0, 0xae, 0x30,
    0xe1, 0xd9, 0x56, 0x38, 0x8e, 0xeb, 0xb2, 0xd2, 0x13, 0xb1, 0x59, 0xa1, 0x31, 0xcc, 0x56, 0xbd,
    0xda, 0x6f, 0x23, 0x7a, 0x21, 0xb3, 0xdd, 0x77, 0x66, 0x65, 0x3a, 0x26, 0xc5, 0xd8, 0xae, 0xf3,
    0xaa, 0x6d, 0xd1, 0x42, 0xa6, 0xb4, 0xe9, 0xf7, 0x12, 0x69, 0x3f, 0x0b, 0x6e, 0x39, 0x4c, 0x9e,
    0x41, 0xb0, 0x31, 0x13, 0xd9, 0x3b, 0xb1, 0xf7, 0x51, 0x1e, 0xbc, 0xf9, 0xb0, 0x55, 0xc3, 0xd2,
    0x74, 0x6f, 0x59, 0x4b, 0x8f, 0xa1, 0x03, 0xea, 0x77, 0xff, 0x20, 0x21, 0xa5, 0xc7, 0x67, 0x95,
    0x9e, 0x6f, 0xcb, 0x71, 0x29, 0x26, 0x76, 0x87, 0x2f, 0xee, 0x26, 0x3d, 0x29, 0xaa, 0x3a, 0x40,
    0xeb, 0xc7, 0xf9, 0x40, 0xfc, 0x90, 0xa8, 0xcf, 0x57, 0x4b, 0x49, 0x5e, 0x85, 0x32, 0xb2, 0x33,
    0x6b, 0x99, 0x56, 0x1e, 0xc2, 0x32, 0xfd, 0x27, 0xff, 0x6d, 0xc3, 0xe5, 0x43, 0x90, 0x39, 0x95,
    0xab, 0xef, 0x0d, 0xde, 0xe0, 0x8a, 0xb0, 0x7d, 0x60, 0x05, 0x04, 0xa9, 0x71, 0x94, 0xf4, 0x1e,
    0xfe, 0x52, 0x0c, 0x7c, 0x1b, 0x28, 0xb7, 0x99, 0x37, 0x0c, 0xe4, 0x95, 0xe1, 0x89, 0xc2, 0xa9,
    0xa5, 0x03, 0x11, 0x7f, 0x67, 0x88, 0xa0, 0xcd, 0x13, 0x60, 0xad, 0x4d, 0x01, 0xb8, 0x30, 0x27,
    0x9e, 0x84, 0x70, 0xc0, 0x68, 0x19, 0xbc, 0x66, 0xae, 0xf8, 0xc5, 0xd7, 0xbd, 0x29, 0x3c, 0x98,
    0x2e, 0x98, 0x18, 0x2e, 0x70, 0x30, 0x3d, 0x26, 0xd6, 0xb0, 0x74, 0xd5, 0x00, 0x6f, 0x31, 0x09,
    0x21, 0xc4, 0xce, 0xaf, 0x1e, 0xfd, 0xf4, 0xa1, 0x61, 0x24, 0xe1, 0x68, 0x94, 0xbd, 0x6d, 0x19,
    0xe2, 0x87, 0x0a, 0xdb, 0x77, 0xe1, 0x80, 0x05, 0x17, 0xe4, 0x9c, 0x76, 0x9f, 0x17, 0x9d, 0x88,
    0x20, 0x60, 0x88, 0xc2, 0xf9, 0x63, 0x38, 0x50, 0x86, 0x4d, 0xb0, 0x6c, 0x81, 0xe0, 0xb2, 0xac,
    0x6f, 0x14, 0xf1, 0x67, 0x03, 0x06, 0x02, 0x97, 0xc8, 0xbf, 0xcd, 0x60, 0xad, 0x8c, 0xb7, 0x93,
    0x56, 0x2e, 0xac, 0x20, 0x80, 0xa8, 0xf2, 0xc1, 0xc0, 0xb4, 0xca, 0x62, 0x0e, 0xad, 0x37, 0x5d,
    0x24, 0xbb, 0xb4, 0x96, 0x71, 0xa7, 0x9c, 0x3c, 0x33, 0x83, 0x20, 0x37, 0xcf, 0x78, 0xd6, 0x61,
    0x6b, 0xb5, 0xca, 0x5e, 0x2b, 0x05, 0x3a, 0x78, 0x4f, 0x8d, 0xf4, 0x5b, 0x9e, 0x39, 0x58, 0x7b,
    0x3d, 0x27, 0x58, 0xf6, 0xba, 0xd7, 0x60, 0xba, 0x3b, 0x14, 0x86, 0xf2, 0x32, 0xdf, 0xe8, 0x0b,
    0xb8, 0xab, 0x28, 0xbd, 0xb1, 0x10, 0xe1, 0xef, 0xa9, 0x06, 0x71, 0x12, 0x94, 0x4f, 0xe8, 0x6b,
    0x45, 0x73, 0x76, 0x06, 0x9e, 0x28, 0x3b, 0x59, 0xbf, 0x16, 0x8b, 0x71, 0x00, 0xca, 0x23, 0xb2,
    0x31, 0xaf, 0x3a, 0xd5, 0x56, 0x85, 0xf2, 0xb3, 0xda, 0xb1, 0x41, 0x39, 0xb9, 0x26, 0x4f, 0x6c,
    0x5e, 0x63, 0x76, 0x60, 0x28, 0xa9, 0x22, 0xca, 0x32, 0xd8, 0x6d, 0xbe, 0xe5, 0x91, 0x69, 0x74,
    0xf3, 0xd9, 0xab, 0x38, 0xe0, 0xc4, 0x6c, 0x69, 0xd4, 0xfb, 0xcd, 0xc9, 0x68, 0x6f, 0x5c, 0x62,
    0x97, 0x30, 0xe7, 0x96, 0x8b, 0xf6, 0x98, 0xbd, 0x3d, 0xa4, 0x71, 0x6a, 0x89, 0x04, 0x26, 0xc6,
    0x4c, 0x98, 0x17, 0x5b, 0x63, 0x6d, 0x4b, 0x39, 0x04, 0x64, 0xa5, 0xe7, 0x9f, 0x9e, 0x83, 0x8e,
    0x2b, 0x79, 0x95, 0x03, 0xea, 0x36, 0xa4, 0x35, 0x5b, 0x2b, 0xaf, 0x75, 0xfd, 0x3d, 0x79, 0x2b,
    0x63, 0x88, 0xe1, 0x34, 0x18, 0xdb, 0x2c, 0x76, 0x06, 0x0b, 0xd8, 0xcc, 0x86, 0x3a, 0x4f, 0xaf,
    0xb6, 0x4c, 0x0c, 0x98, 0x24, 0xee, 0xb3, 0x79, 0x18, 0x5e, 0x94, 0xda, 0xa6, 0x86, 0x09, 0x8e,
    0x48, 0xc1, 0x0e, 0xad, 0xfb, 0x85, 0x46, 0x8c, 0x13, 0x9d, 0x34, 0xec, 0x44, 0xf5, 0x37, 0x23,
    0xf3, 0x5f, 0x43, 0x4c, 0x12, 0x90, 0x45, 0x46, 0xda, 0x6d, 0xb6, 0x48, 0x50, 0xd1, 0x41, 0x16,
    0xe3, 0x79, 0x83, 0x93, 0x63, 0x1a, 0xb1, 0x12, 0xd5, 0x3a, 0xe0, 0xa1, 0x32, 0xf8, 0x74, 0x19,
    0xc1, 0xaa, 0xa3, 0x87, 0x6b, 0xe2, 0xdf, 0x79, 0x22, 0xa3, 0xc3, 0x08, 0xbe, 0x7b, 0xce, 0xff,
    0xa0, 0x73, 0xc7, 0x7d, 0x06, 0x4d, 0x4a, 0xed, 0x47, 0xe3, 0xc5, 0xbb, 0x3f, 0xde, 0x83, 0x88,
    0xf5, 0xde, 0xaf, 0x81, 0xc1, 0x32, 0x3d, 0xd8, 0xa0, 0xb6, 0x5c, 0x65, 0x65, 0x7c, 0xfc, 0x3b,
    0xf9, 0x71, 0x81, 0xcb, 0x98, 0xd2, 0xad, 0x69, 0xea, 0x9f, 0x4f, 0x88, 0x1a, 0x4c, 0xbf, 0x9d,
    0xd3, 0x28, 0x43, 0xa0, 0xe5, 0x61, 0x09, 0x52, 0x5b, 0x3f, 0x86, 0xe0, 0x35, 0x6b, 0xd5, 0x80,
    0x17, 0xd8, 0x3c, 0x2b, 0xde, 0xeb, 0xcf, 0x3e, 0x4a, 0xea, 0x56, 0x80, 0xd8, 0xcd, 0xda, 0x7d,
    0x03, 0xfa, 0xf3, 0xcc, 0xb2, 0x28, 0xb8, 0x3b, 0xd8, 0xfa, 0xd6, 0x5e, 0x6a, 0x7f, 0xd1, 0xde,
    0x6e, 0xbd, 0x36, 0x64, 0x59, 0x73, 0xc4, 0xee, 0xff, 0x4d, 0x18, 0xba, 0x49, 0x2c, 0x49, 0x72,
    0xf2, 0xb8, 0x21, 0x26, 0x4a, 0xf6, 0x14, 0xf2, 0xcc, 0x69, 0x21, 0x2d, 0xde, 0xce, 0xc0, 0x78,
    0x72, 0xe6, 0x8a, 0x23, 0x1c, 0x3b, 0x80, 0x7f, 0x6c, 0xe1, 0x92, 0x9b, 0x4b, 0xd8, 0xea, 0x54,
    0xc5, 0xd2, 0x32, 0xc0, 0xc0, 0x76, 0x07, 0x80, 0x02, 0x90, 0x0a, 0x36, 0xa8, 0x08, 0x57, 0x10,
    0x74, 0x28, 0xf7, 0x23, 0x25, 0x3e, 0x89, 0x7d, 0x1d, 0x50, 0xa9, 0x20, 0x14, 0xc6, 0xf7, 0xf4,
    0xa2, 0x75, 0x78, 0xa7, 0x7f, 0x2b, 0xe6, 0x66, 0x1b, 0x7c, 0xfa, 0xa5, 0xdf, 0xd6, 0xac, 0xdc,
    0x2d, 0x96, 0x8a, 0x07, 0x1e, 0x5e, 0x74, 0xbb, 0x21, 0x92, 0x13, 0xb0, 0xfc, 0xb7, 0xd4, 0xb6,
    0x4e, 0x0e, 0x26, 0x17, 0x93, 0x7f, 0x26, 0x86, 0x64, 0xdc, 0x74, 0xd3, 0xd2, 0xd4, 0x09, 0x83,
    0x58, 0xa6, 0xee, 0xc1, 0xc1, 0x29, 0xb3, 0x5a, 0xb5, 0x44, 0xff, 0x07, 0x15, 0xce, 0x07, 0xb9,
    0x3d, 0x2a, 0xaa, 0x9c, 0x76, 0x7e, 0xec, 0x69, 0xf9, 0x01, 0xb6, 0x5b, 0x56, 0x58, 0x5e, 0xcd,
    0x19, 0x5b, 0xd5, 0x7e, 0x6d, 0x32, 0x8a, 0x3d, 0x85, 0xb9, 0xe2, 0x94, 0x0c, 0x30, 0xd9, 0xd0,
    0xca, 0x94, 0x37, 0xf8, 0xec, 0x20, 0x17, 0x0f, 0x91, 0xb8, 0xdc, 0xa9, 0x9a, 0x11, 0xd3, 0xb5,
    0xaa, 0x1e, 0xbb, 0x78, 0x34, 0xf5, 0x15, 0x7f, 0x36, 0x78, 0x61, 0xd6, 0x69, 0xb6, 0xdc, 0x76,
    0x17, 0xac, 0x50, 0xae, 0x19, 0x15, 0x28, 0xfc, 0x02, 0x2c, 0x53, 0x74, 0x37, 0xe7, 0x1f, 0xad,
    0xef, 0x67, 0x5f, 0x63, 0x40, 0x15, 0xe9, 0xb1, 0x35, 0x33, 0x10, 0x34, 0x88, 0x92, 0x52, 0x1f,
    0x12, 0x70, 0xd8, 0x27, 0xbd, 0x08, 0x44, 0x49, 0x11, 0x08, 0xe3, 0x3b, 0x10, 0x60, 0x8e, 0xc4,
    0x48, 0x15, 0x58, 0xd6, 0xe2, 0xd6, 0xf6, 0xc5, 0x86, 0x2f, 0x29, 0xff, 0xae, 0x6c, 0xc6, 0x3d,
    0x6e, 0x7c, 0x71, 0x1c, 0x7e, 0x0e, 0x3f, 0xfd, 0x8a, 0x9b, 0x15, 0xab, 0x50, 0xb0, 0x9f, 0x8a,
    0xd9, 0x08, 0x63, 0x0f, 0x13, 0x6e, 0x28, 0xa2, 0xf6, 0x85, 0xc2, 0x42, 0x12, 0x0a, 0x58, 0x60,
    0x7c, 0x5f, 0xe3, 0x23, 0x19, 0x46, 0x16, 0x62, 0x1d, 0xd1, 0xf7, 0xe4, 0xf1, 0xa1, 0xf3, 0x7b,
    0x33, 0x4f, 0x56, 0x8f, 0xa5, 0x86, 0x27, 0x26, 0x94, 0xcd, 0x89, 0x09, 0xb5, 0x10, 0x16, 0x19,
    0x3e, 0x55, 0x94, 0x5d, 0xff, 0xd0, 0xb3, 0x09, 0x62, 0xc8, 0x19, 0xa3, 0xd4, 0xb1, 0xc7, 0x52,
    0x55, 0x46, 0xd7, 0x26, 0x61, 0xbd, 0x65, 0x14, 0xf4, 0xd3, 0x3b, 0x7d, 0xdf, 0x10, 0xb2, 0xfb,
    0xee, 0x74, 0xfc, 0xbe, 0x5a, 0x42, 0xce, 0x24, 0x52, 0x81, 0xe6, 0xc5, 0x35, 0x02, 0xdb, 0x68,
    0x60, 0x31, 0x84, 0x88, 0x6a, 0xde, 0xfb, 0x96, 0x45, 0x13, 0x45, 0x4b, 0x29, 0x6e, 0xfe, 0x4d,
    0x4c, 0x79, 0x39, 0xd2, 0x52, 0x52, 0x8b, 0x2f, 0x8c, 0xc1, 0xf6, 0x06, 0x05, 0x63, 0xc1, 0xff,
    0xe7, 0xee, 0xf6, 0xa1, 0xa5, 0xae, 0xc5, 0xdd, 0xe6, 0xd8, 0x3d, 0x13, 0xd9, 0x37, 0x10, 0x24,
    0x09, 0x7e, 0xcf, 0x01, 0x10, 0xca, 0x8b, 0x40, 0x13, 0x26, 0x2d, 0xf0, 0xa9, 0x92, 0x48, 0x24,
    0x28, 0x9b, 0x0d, 0x64, 0xe8, 0x4a, 0x61, 0xe9, 0x40, 0xe9, 0x12, 0x33, 0xac, 0xc4, 0x31, 0xff,
    0x92, 0xdb, 0x92, 0x56, 0xb7, 0x4a, 0x10, 0x16, 0xe2, 0x54, 0xc4, 0xfa, 0x58, 0xbd, 0xbe, 0xf2,
    0x3e, 0x80, 0x62, 0xbd, 0xe3, 0xdc, 0xf0, 0x3b, 0x62, 0x96, 0x75, 0xd7, 0x98, 0x1a, 0x6b, 0xd4,
    0x24, 0x9e, 0xab, 0x82, 0x8e, 0xb8, 0x7a, 0x15, 0x90, 0x90, 0xfe, 0xc9, 0x97, 0xd7, 0x24, 0x7f,
    0x88, 0x7e, 0xaf, 0xfa, 0x51, 0x5e, 0x80, 0x6f, 0xda, 0x72, 0x94, 0x69, 0xe5, 0x8b, 0x54, 0x03,
    0x4a, 0x1a, 0x19, 0xb1, 0xf2, 0x52, 0x5b, 0xe9, 0xb7, 0xba, 0xa7, 0xa9, 0x71, 0x3d, 0x9c, 0x27,
    0x68, 0x05, 0x97, 0xd4, 0x35, 0x19, 0x78, 0x76, 0x49, 0x1a, 0xcc, 0x5f, 0x11, 0xfb, 0x7d, 0x1a,
    0x13, 0xa5, 0x4d, 0x64, 0xda, 0x04, 0x1c, 0x26, 0xe8, 0x25, 0xc9, 0x7d, 0x20, 0x48, 0x37, 0x82,
    0xe6, 0xed, 0xf0, 0x43, 0x35, 0x5a, 0xdc, 0x32, 0xed, 0x37, 0x12, 0xc8, 0x6b, 0xd4, 0xe7, 0xeb,
    0xf2, 0xee, 0x83, 0xf9, 0x25, 0x52, 0xca, 0xd0, 0x52, 0x06, 0x2d, 0x91, 0xa5, 0xa1, 0x45, 0x26,
    0xb5, 0x49, 0x90, 0xda, 0xc3, 0x18, 0x51, 0x01, 0xf9, 0xdd, 0x91, 0x2a, 0x77, 0x67, 0x16, 0xff,
    0x58, 0xa9, 0x75, 0xae, 0x11, 0xff, 0xfc, 0xa5, 0xf6, 0x6f, 0x3f, 0xa5, 0x13, 0xbe, 0x3e, 0xb6,
    0xb3, 0x8a, 0x84, 0x57, 0xf4, 0x9d, 0x88, 0x8f, 0x28, 0x4d, 0x38, 0xb7, 0x23, 0xf1, 0x1c, 0x6a,
    0x03, 0x95, 0xd4, 0xe2, 0xef, 0x2c, 0x9c, 0xec, 0x5d, 0x31, 0x7f, 0xd1, 0x4b, 0x89, 0xd8, 0xd1,
    0xa2, 0x56, 0x2b, 0x6d, 0xac, 0xa7, 0x7d, 0xb9, 0x0d, 0xd3, 0x32, 0x48, 0x41, 0x2d, 0xff, 0x84,
    0x45, 0x93, 0xc8, 0x51, 0xd0, 0x73, 0x8c, 0x46, 0x04, 0x52, 0xe2, 0x41, 0x53, 0x7d, 0x8d, 0xbc,
    0x18, 0x16, 0xe6, 0x80, 0x89, 0xe8, 0x94, 0xf9, 0x75, 0x2c, 0xb5, 0xd9, 0xeb, 0x8a, 0x82, 0x6e,
    0xb0, 0x22, 0xa7, 0x2d, 0xf5, 0xf1, 0x03, 0x81, 0x96, 0xf5, 0xfa, 0xc7, 0xe9, 0x64, 0x05, 0x8a,
    0xf8, 0x5c, 0xc3, 0xe3, 0x1b, 0xab, 0xfa, 0xa6, 0xc2, 0x6e, 0xf9, 0x83, 0x66, 0x80, 0x8f, 0x11,
    0xec, 0xbc, 0x0d, 0x49, 0x66, 0x76, 0xf4, 0x8f, 0x51, 0x09, 0x70, 0x42, 0x7e, 0x48, 0x8b, 0x93,
    0x8c, 0x9d, 0xe3, 0x20, 0xab, 0xaa, 0x2b, 0xc0, 0x4c, 0x2f, 0x86, 0x0f, 0x6e, 0x40, 0x59, 0x0c,
    0x1c, 0xcd, 0x82, 0x0a, 0xf2, 0x6e, 0xab, 0xe4, 0x2b, 0xbd, 0x2a, 0x5a, 0xe3, 0x4d, 0x2b, 0xee,
    0x6b, 0x61, 0x35, 0xc9, 0x7a, 0xcb, 0x92, 0x71, 0x82, 0x6d, 0xd6, 0xd9, 0xbd, 0xeb, 0xcf, 0x76,
    0x19, 0xb9, 0x36, 0x76, 0xe0, 0x37, 0x4d, 0x1b, 0xb1, 0xd8, 0x69, 0x16, 0xfa, 0xe9, 0xa5, 0xb5,
    0x82, 0xea, 0xcc, 0xef, 0x77, 0x08, 0x41, 0xc7, 0x4c, 0x86, 0xc1, 0x94, 0x14, 0xab, 0xc4, 0x14,
    0xb0, 0xbb, 0xae, 0x10, 0x19, 0xc3, 0xf1, 0x8a, 0xf2, 0xe0, 0x40, 0xef, 0x12, 0x1a, 0xaa, 0x58,
    0x0d, 0xff, 0xdd, 0x8c, 0x52, 0xe5, 0x77, 0xe9, 0x3b, 0x79, 0x2a, 0x94, 0xfa, 0x0e, 0x07, 0x94,
    0xf2, 0xb4, 0x97, 0xf5, 0xb9, 0x04, 0x2f, 0x84, 0xcc, 0x4a, 0x91, 0x92, 0x79, 0xc5, 0x32, 0x69,
    0x30, 0x8c, 0xe0, 0xa3, 0x11, 0x94, 0xd5, 0x15, 0xa7, 0x2e, 0xe8, 0xec, 0x31, 0x8d, 0x52, 0x91,
    0x60, 0x14, 0xfc, 0x3d, 0x46, 0xa8, 0xe7, 0xba, 0x0a, 0x7f, 0x6b, 0x7d, 0x2e, 0x30, 0x82, 0x05,
    0x52, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x07, 0x01, 0xa0, 0x82, 0x05, 0x43,
    0x04, 0x82, 0x05, 0x3f, 0x30, 0x82, 0x05, 0x3b, 0x30, 0x82, 0x05, 0x37, 0x06, 0x0b, 0x2a, 0x86,
    0x48, 0x86, 0xf7, 0x0d, 0x01, 0x0c, 0x0a, 0x01, 0x02, 0xa0, 0x82, 0x04, 0xe6, 0x30, 0x82, 0x04,
    0xe2, 0x30, 0x1c, 0x06, 0x0a, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x0c, 0x01, 0x03, 0x30,
    0x0e, 0x04, 0x08, 0x9c, 0x6f, 0x8a, 0xd7, 0xbb, 0x49, 0x05, 0x89, 0x02, 0x02, 0x08, 0x00, 0x04,
    0x82, 0x04, 0xc0, 0xcc, 0x5c, 0x53, 0xd0, 0x43, 0xa0, 0x87, 0x8a, 0x2f, 0xea, 0x54, 0xc4, 0x10,
    0x58, 0x1b, 0xf2, 0x02, 0x16, 0x99, 0x36, 0x4a, 0xb8, 0x2c, 0x53, 0x5a, 0x43, 0x3d, 0x3b, 0x39,
    0xd9, 0x9b, 0x81, 0x4b, 0xfb, 0xf2, 0xd8, 0x90, 0xdb, 0xe1, 0xb5, 0x93, 0x4c, 0x11, 0x7f, 0xc8,
    0xf8, 0xad, 0xbd, 0xf8, 0xf2, 0xf8, 0x97, 0xe4, 0xcd, 0xa9, 0x61, 0x65, 0xc8, 0x23, 0x22, 0xf0,
    0x4d, 0xc1, 0xd5, 0x87, 0x0b, 0xee, 0x9d, 0x40, 0x27, 0xb8, 0x3c, 0xaf, 0x2f, 0xa7, 0xb7, 0x30,
    0x81, 0xb3, 0x4b, 0x42, 0x11, 0xfd, 0x05, 0xcf, 0x6b, 0xe8, 0xe2, 0xa0, 0x7d, 0x2a, 0x5e, 0xfa,
    0xd4, 0x14, 0xb8, 0xdf, 0xe5, 0xa4, 0xbd, 0x56, 0x46, 0xf7, 0xc5, 0xc3, 0x1b, 0x4f, 0xa2, 0x02,
    0x4b, 0x97, 0xb0, 0x40, 0xd2, 0x7b, 0x80, 0xfe, 0x1f, 0x91, 0xb3, 0x47, 0xee, 0x57, 0x49, 0x26,
    0x74, 0xac, 0xb9, 0xfe, 0x5f, 0x16, 0xb5, 0x8c, 0x5d, 0x98, 0x40, 0xda, 0xe5, 0x5e, 0xf6, 0xe7,
    0x5e, 0x8f, 0x3d, 0xe2, 0xbe, 0x5e, 0xac, 0xec, 0xb2, 0xf1, 0x31, 0x98, 0x1c, 0x59, 0xa4, 0x43,
    0x00, 0x57, 0x5d, 0xc6, 0x07, 0x8a, 0xa0, 0xe0, 0xc3, 0xa5, 0x9a, 0xb9, 0xe5, 0xb2, 0xe3, 0x93,
    0x29, 0x40, 0x0c, 0xe3, 0x3f, 0x44, 0x27, 0xf6, 0xc7, 0x31, 0xa0, 0xb1, 0x7a, 0xe5, 0xf2, 0xb5,
    0xb4, 0x32, 0x2a, 0xb7, 0xba, 0x02, 0x48, 0x4d, 0x6f, 0x89, 0x9c, 0x11, 0x97, 0x3c, 0x20, 0x0d,
    0x62, 0xae, 0x7d, 0x41, 0x5d, 0x9e, 0x90, 0x72, 0xd4, 0xe1, 0x33, 0x8d, 0xab, 0xbb, 0x0a, 0x19,
    0xc8, 0xe3, 0xb1, 0xf3, 0xb6, 0x2c, 0x0e, 0x59, 0x55, 0x79, 0xe7, 0x69, 0x6b, 0x35, 0x1f, 0xb5,
    0x0e, 0x2b, 0x96, 0xbe, 0xa4, 0x8f, 0xcb, 0x84, 0xe8, 0x87, 0xec, 0x5f, 0x09, 0x9a, 0xec, 0x7a,
    0xb6, 0x3d, 0x4e, 0x00, 0x62, 0x7f, 0x7d, 0x4d, 0x48, 0x83, 0x96, 0xd3, 0x45, 0x5e, 0xc0, 0x31,
    0x2c, 0x77, 0xa9, 0x76, 0x65, 0x7e, 0x53, 0x31, 0x9d, 0xcd, 0x23, 0xa3, 0x7a, 0x9f, 0x17, 0x0a,
    0xf9, 0x64, 0xb2, 0x07, 0x40, 0xe5, 0xef, 0x45, 0x7a, 0xd5, 0x1e, 0xa2, 0x54, 0x4f, 0x44, 0x91,
    0x9a, 0xc6, 0xec, 0xe5, 0x70, 0x18, 0x4d, 0xb0, 0x22, 0x23, 0xd7, 0x6b, 0x46, 0x55, 0x0e, 0xb8,
    0x0f, 0x72, 0x36, 0xe4, 0x03, 0xe2, 0x29, 0x81, 0x99, 0x28, 0x64, 0x6b, 0xe8, 0x5c, 0xc8, 0x6f,
    0xf6, 0x7a, 0xbb, 0xb7, 0xa7, 0x8c, 0x24, 0x22, 0x26, 0x5d, 0x7a, 0xab, 0x9e, 0xb1, 0xe3, 0xe4,
    0x43, 0x08, 0xd9, 0xc1, 0xc1, 0x7e, 0xa3, 0xb9, 0xfe, 0x41, 0x12, 0x91, 0xfc, 0x61, 0x6f, 0x3a,
    0x3e, 0x75, 0xaa, 0x70, 0x3a, 0x49, 0x59, 0xb0, 0xa9, 0xd4, 0x73, 0xad, 0x31, 0x97, 0xd2, 0x67,
    0xba, 0xe4, 0xd5, 0x0c, 0x8e, 0xb2, 0xf4, 0x5a, 0xda, 0xdf, 0xeb, 0x66, 0xc9, 0xd1, 0x7b, 0x1d,
    0xf7, 0x37, 0x82, 0xc8, 0xa4, 0x70, 0x8d, 0xf4, 0xb5, 0xfd, 0x86, 0x78, 0x15, 0x76, 0xa0, 0x23,
    0xe1, 0xca, 0xf0, 0xe3, 0x82, 0xed, 0x21, 0x01, 0x44, 0xa5, 0xcc, 0x48, 0xcb, 0x58, 0x68, 0x8e,
    0xa0, 0x0b, 0xac, 0x01, 0xb3, 0x65, 0xa3, 0x3b, 0x8d, 0x60, 0x5e, 0x5b, 0x56, 0xc2, 0xcd, 0x15,
    0x51, 0xcb, 0x3e, 0x38, 0x31, 0x50, 0x2e, 0xb9, 0x8e, 0x65, 0x86, 0x42, 0x32, 0x59, 0x01, 0xeb,
    0x20, 0xfe, 0xad, 0x9a, 0xb8, 0xdf, 0x17, 0xbe, 0xd8, 0xf5, 0x9d, 0x15, 0x98, 0xf1, 0x43, 0x7c,
    0x24, 0xee, 0x6b, 0xf4, 0x24, 0x52, 0x80, 0x8d, 0x12, 0x51, 0xea, 0x2f, 0xfe, 0x1a, 0xa0, 0xf6,
    0x17, 0x36, 0x62, 0x14, 0xe0, 0xa9, 0xa6, 0x34, 0xd6, 0x34, 0x2f, 0x6f, 0x75, 0xec, 0xf9, 0xbf,
    0x97, 0x4e, 0x96, 0x94, 0x36, 0x2b, 0x96, 0xc2, 0x0a, 0x53, 0x4d, 0x62, 0x40, 0x01, 0x21, 0xfd,
    0x69, 0xec, 0x13, 0x87, 0x14, 0xdc, 0x37, 0x91, 0x4e, 0x30, 0x3a, 0x1e, 0x56, 0xb8, 0xa3, 0x2e,
    0x63, 0x69, 0x34, 0xd5, 0x59, 0x77, 0xef, 0x7d, 0x44, 0x1e, 0xf3, 0xda, 0x16, 0x32, 0x57, 0xf5,
    0x01, 0x1f, 0x29, 0x3c, 0x64, 0xf3, 0x20, 0xb8, 0xcf, 0x19, 0xf0, 0x29, 0x45, 0x01, 0x36, 0x15,
    0xb1, 0xe5, 0xb1, 0x6d, 0x23, 0xc9, 0xd7, 0x80, 0x5a, 0xbf, 0x35, 0x78, 0x9d, 0x55, 0x2e, 0xf4,
    0x38, 0x95, 0x02, 0x50, 0xb1, 0xe8, 0xfa, 0x41, 0xa7, 0x11, 0x8b, 0x24, 0xbf, 0xad, 0xfd, 0x53,
    0x7c, 0xdb, 0xca, 0xc6, 0x9b, 0xa0, 0x99, 0xfb, 0xa3, 0x2b, 0xd6, 0x6f, 0x61, 0xc7, 0xea, 0x62,
    0x34, 0xb8, 0xe5, 0x12, 0xfb, 0xd1, 0x57, 0xe5, 0xdb, 0x99, 0x96, 0x83, 0x37, 0x4f, 0xc0, 0x9b,
    0x9c, 0x30, 0x46, 0x5d, 0xfe, 0x48, 0xa6, 0xae, 0xdc, 0x9b, 0x9c, 0xa6, 0xd7, 0x11, 0xaa, 0xb9,
    0x07, 0xad, 0xc8, 0xe4, 0xb0, 0xde, 0xdc, 0x4b, 0xdc, 0xa5, 0x7f, 0xf0, 0xab, 0x3b, 0x35, 0x6b,
    0x93, 0x6c, 0xf9, 0xa1, 0x68, 0x4a, 0x31, 0xf9, 0xf6, 0x9f, 0x2d, 0x3a, 0x4a, 0x9d, 0x8e, 0xd2,
    0xba, 0x56, 0xbb, 0xd9, 0x31, 0x43, 0x20, 0x28, 0x65, 0xd0, 0x19, 0x88, 0xcd, 0x52, 0x92, 0xf3,
    0x51, 0x0a, 0x1e, 0xe3, 0xb3, 0x45, 0x21, 0xa9, 0x90, 0x35, 0xb1, 0xb0, 0x18, 0x92, 0x9d, 0xb2,
    0xf5, 0x83, 0xb0, 0xe5, 0x84, 0xf2, 0x32, 0xe2, 0xb1, 0x41, 0x15, 0xcb, 0x39, 0xb5, 0x92, 0x6d,
    0xaa, 0x39, 0x8a, 0x47, 0x07, 0xc7, 0xde, 0x8d, 0x33, 0x43, 0x08, 0x98, 0x41, 0x84, 0xc6, 0xed,
    0x3e, 0x56, 0xbb, 0x15, 0xe3, 0x14, 0x3f, 0xa8, 0xf8, 0x2c, 0x2f, 0xa3, 0x96, 0x0d, 0xe5, 0x54,
    0xc3, 0xc3, 0x80, 0x1b, 0xad, 0x1e, 0xc3, 0x9e, 0x75, 0x5d, 0x97, 0x5c, 0x08, 0x1c, 0x1f, 0xca,
    0xa9, 0x1c, 0x12, 0xdb, 0xd3, 0xe0, 0x7d, 0x7a, 0xf4, 0xc1, 0xb8, 0x4c, 0xe8, 0x1e, 0xc4, 0x6a,
    0x5f, 0x89, 0x12, 0x10, 0xdc, 0x44, 0xea, 0x89, 0x3a, 0x40, 0x46, 0xb7, 0x9f, 0xfb, 0xef, 0x33,
    0x5e, 0xe9, 0xc6, 0x17, 0xc8, 0x43, 0x1f, 0xe0, 0xdb, 0x92, 0x21, 0xb4, 0xaf, 0x40, 0x89, 0xce,
    0xff, 0x44, 0xa4, 0x8f, 0x60, 0x76, 0xa2, 0x65, 0x43, 0x39, 0xd5, 0xe1, 0x13, 0x03, 0xe0, 0x50,
    0x72, 0xad, 0xb3, 0x83, 0x3d, 0x99, 0xdb, 0x9f, 0xe8, 0x7c, 0x4e, 0x4d, 0xfe, 0xa6, 0x42, 0x41,
    0xe3, 0xbb, 0x42, 0x69, 0x1b, 0x44, 0x5d, 0x08, 0x98, 0xc0, 0x45, 0xc3, 0x5e, 0x3c, 0x79, 0x0d,
    0xc3, 0x06, 0x52, 0xdf, 0x36, 0x74, 0x45, 0x11, 0x53, 0xbe, 0x5d, 0xba, 0x67, 0x3f, 0xcc, 0x5a,
    0x15, 0x87, 0x3d, 0xba, 0x8e, 0xa2, 0x95, 0x53, 0x48, 0xe1, 0xe8, 0x56, 0xcc, 0x1a, 0xf6, 0x67,
    0x67, 0xd4, 0x64, 0x14, 0x69, 0xbe, 0x24, 0x6e, 0xa7, 0x82, 0xa3, 0xb5, 0x47, 0xb0, 0x94, 0x25,
    0x21, 0x58, 0x2a, 0x71, 0x10, 0xd1, 0xe8, 0xba, 0xf4, 0x32, 0xe8, 0x39, 0xef, 0x20, 0xb6, 0xb7,
    0xf1, 0xff, 0xf2, 0xc9, 0x28, 0x2a, 0x09, 0x64, 0x1a, 0x7f, 0x3b, 0x8a, 0xad, 0x4a, 0x76, 0x0a,
    0x3e, 0x6b, 0x23, 0xbe, 0xef, 0x03, 0xbd, 0x6c, 0x23, 0xaf, 0x1b, 0x0a, 0xd5, 0x87, 0x06, 0x85,
    0x6a, 0xff, 0x00, 0x14, 0x08, 0x1b, 0x95, 0xb0, 0x3c, 0x3a, 0xe3, 0x86, 0x49, 0xa9, 0xb9, 0x5d,
    0xc8, 0xdb, 0x53, 0xfd, 0x1e, 0x28, 0x4e, 0xe8, 0x69, 0x35, 0x5e, 0x2c, 0xfd, 0xf9, 0x8c, 0x16,
    0xfb, 0xa0, 0x80, 0x4c, 0xef, 0xc0, 0x69, 0x3f, 0x4a, 0xd2, 0xb7, 0xc4, 0xee, 0x99, 0x62, 0xa8,
    0x5a, 0x11, 0xa3, 0x9a, 0x60, 0x98, 0xe1, 0x6a, 0x11, 0x3a, 0xf0, 0x63, 0xe2, 0x2e, 0x16, 0xee,
    0x97, 0x8c, 0xf1, 0xd7, 0x0d, 0x2b, 0xc8, 0x86, 0x5d, 0xe2, 0x15, 0xeb, 0x18, 0x0c, 0xa4, 0xbc,
    0xe2, 0x7a, 0x70, 0x5c, 0x51, 0xb2, 0x3f, 0x7c, 0x4d, 0x93, 0x92, 0xe0, 0x60, 0xac, 0x83, 0xb0,
    0x50, 0x3d, 0xfb, 0x48, 0xfe, 0xf8, 0x1e, 0xd4, 0x10, 0x95, 0xd3, 0x58, 0xd9, 0x83, 0xeb, 0x3b,
    0xd2, 0xda, 0x4f, 0xaf, 0x6a, 0xf6, 0xfa, 0xe9, 0x11, 0x89, 0xc5, 0xee, 0xd7, 0x81, 0x2d, 0x68,
    0x78, 0x20, 0x54, 0xf4, 0x9c, 0x48, 0x33, 0xf1, 0x96, 0xe8, 0x0d, 0xb2, 0x64, 0x6c, 0x21, 0xe2,
    0x51, 0xba, 0xd1, 0xdf, 0x06, 0x49, 0x37, 0x15, 0xad, 0x67, 0xa2, 0xd9, 0x76, 0xbd, 0xcf, 0xa8,
    0xf2, 0xa0, 0x03, 0x80, 0xd5, 0xa3, 0x4e, 0x11, 0x96, 0xcf, 0x4d, 0x9d, 0xe7, 0x11, 0xb1, 0x94,
    0x34, 0xc4, 0x66, 0x85, 0x31, 0xf9, 0xec, 0xc5, 0x01, 0xd5, 0xa2, 0x1d, 0xde, 0x7f, 0xa1, 0xe9,
    0xbe, 0xe4, 0x31, 0x12, 0xb3, 0x23, 0x73, 0xd1, 0xa8, 0x68, 0x98, 0x46, 0xa3, 0x3d, 0x6d, 0x48,
    0x79, 0x52, 0x14, 0xb7, 0xf3, 0x23, 0xea, 0xd8, 0xd1, 0x5c, 0xf4, 0x29, 0x4e, 0xa5, 0x31, 0x10,
    0x4c, 0x40, 0x4b, 0xd3, 0x80, 0x77, 0x9d, 0x46, 0xb0, 0xd1, 0x9a, 0x40, 0xb1, 0x1e, 0xec, 0xd2,
    0x83, 0x31, 0xaa, 0x31, 0x3e, 0x30, 0x17, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01,
    0x09, 0x14, 0x31, 0x0a, 0x1e, 0x08, 0x00, 0x74, 0x00, 0x61, 0x00, 0x69, 0x00, 0x6c, 0x30, 0x23,
    0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x09, 0x15, 0x31, 0x16, 0x04, 0x14, 0x92,
    0xb9, 0xb0, 0x50, 0xa4, 0xf2, 0xc2, 0xc5, 0x5d, 0x69, 0x84, 0x96, 0x96, 0xae, 0xbc, 0x79, 0x49,
    0x43, 0xd1, 0x76, 0x30, 0x31, 0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a,
    0x05, 0x00, 0x04, 0x14, 0x2f, 0xdb, 0xb5, 0xb5, 0x07, 0xe8, 0xf8, 0x25, 0xf5, 0x7d, 0x8d, 0x77,
    0x97, 0xfc, 0x98, 0x10, 0x70, 0x9b, 0xb6, 0xfb, 0x04, 0x08, 0x47, 0xbe, 0xc5, 0x4c, 0xcd, 0x04,
    0x34, 0x88, 0x02, 0x02, 0x08, 0x00
};
unsigned int chain_p12_size = 3526;

const std::vector<char> valid_chain_p12 = { reinterpret_cast<const char*>(chain_p12),
                                            reinterpret_cast<const char*>(chain_p12) +
                                                chain_p12_size };

const std::string valid_private_key = R"(
-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCyvFIsLunp+0nK
hu29PAn2dO63VkzOIw1zwuIXV8cOQmHrw/L8fX/+8CBymL+3SEBrJL7KJh0GCzEr
vzMqnGpDQbOZooDpvTvevphUq+fIa1sFEempwzck9VHVYEhG80c8VM+uhE8DHg7Y
PIfLT2HLLnMi7nYXZ4hIHbmNT4+kfRgiQ7yu8V79yZkpfJ8zkrJHye8JFSfCbRog
1yolhUMOIIyDq22yTHING6gotKJ2Lv53yBwFe0RS079FLuu3aBluLrPC9U7SGIKc
pVPPNM3bMgpDRBmZabzkAXF1OTcsydaw6rr7aQxjAvtKWCdy8dj2aElhx7FhFyTL
NB+/AdPLAgMBAAECggEAJ9pv5CQtyhta78q4NequOgMGu2CFzazpMfexXA0ZeGd6
2AwgIQaGxLycL5E5pk949asC06Y98HwMYbnHWef4lbiPqGEgp32nXNpYswCFtR2i
9PVyiYTaxeXhcld+pjtWlfA/QqWDzKkmHDca/E1d++UGjTJoqH1QWOwv1H96Au31
4eT74wjMleiQlf+uR2u6AsPc7uOB6E8ULju7/BiAMD21s4rMyIWn+UbCWmyRKcaR
hIr5BWy5Xr5SZgQNL11x4KhEGCjJGLInxDhqZhGiBQyD0ClacyaGEiaC1P4Q/I/t
9RgZdK3rqB4X0FNWusTdn+Vrn5MICT2q0qDe6yZweQKBgQDrOFG6GUyFYpFc3Xq0
7ntg6FuwRyYd5TWZyvK5cCvIhNQQVRtpLYIAoQ7hf457ixXbalw4gFshh/FVxZZk
57IwOmuuBpUBskldfkBHBmcdsPGnSDMAa0niKna6KUJ0G/7mGx4cY1eLExJDbY1t
V3lJypud0iVyxanNsGxn++hofwKBgQDChpB81FBLTraNyKXigeF1PgrVBPPOowUk
5nUs7oWaaEKoe2z1Bs52skTygbdIosA5gmcCOSbQaWGK3ZNR7suPS3HK9A1ze039
StGeotuGvTBFm41dbIs46SW4p4SlV8wavA8cVEg6PQrFB3bzq/RN2SVOL9AUhcd9
dxvZrYcOtQKBgQCtzn3FT3BVl1HmFtnW2+la4BbwGIK30Ghc7bORBquzjULNlrWc
cD4BjQDb608zRsCt0te2AFJWYocXY9sPUI1AScrLWp28FStU5DdGxBppvBe0Dgtx
odWEQvBb+qTZ+t3M1fjX7SgA4eS64jaAtwQuXIHEikWVmy0vic9wvpkqrQKBgFHD
tBn0OCffVxZzn159D2JziKQPZ9eUaEYhZGFVhOzpJBOjhaHckY4M0rRIV9z9I+VI
bbnegfUaRnjTN+g5gnCh2pvfR4Qh8R8lgsS2WaXiAddQUfRR+pcaUNOz/iptpAoc
wBE9T/rCy7MTtyknPxI10ttxd3oY3Uhcd2Vg7iL9AoGAPYna71oXKe43W4PvtbI+
lDT6qLpQIqtT8KBD2Sdb2RGOMNCxGwU0JjWEe8xNerhi3MY+raqrGW4+ERWpYzBF
XQS7zQePBFck+Lj8bTsokxpCPOJrSa1cg6ABhSa2nM3xF1V8B9usy1nVFycyeYjY
hpH8RR55Z+Dg7NQsuD+VJns=
-----END PRIVATE KEY-----
)";