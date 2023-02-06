#pragma once
#include <library/cpp/http/simple/http_client.h>
#include <util/string/printf.h>

namespace {
    const TString CERT_HEADER = "-----BEGIN CERTIFICATE-----\n";
    const TString CERT_FOOTER = "-----END CERTIFICATE-----\n";
    const TString PRIVKEY_HEADER = "-----BEGIN PRIVATE KEY-----\n";
    const TString PRIVKEY_FOOTER = "-----END PRIVATE KEY-----\n";
}

class TCertificationAuthority {
    public:
        TCertificationAuthority(ui16 caServerPort)
            : HttpClient("localhost", caServerPort)
            {}

        void SignCertificate(const TString& host, TString& cert, TString& privkey) {
            TStringStream stream;
            HttpClient.DoGet(Sprintf("/sign?host=%s", host.data()), &stream);
            const TString& data = stream.Str();
            auto ch = data.find(CERT_HEADER);
            if (ch != TString::npos) {
                auto cf = data.find(CERT_FOOTER, ch + CERT_HEADER.size());
                if (cf != TString::npos) {
                    auto pkh = data.find(PRIVKEY_HEADER, cf + CERT_FOOTER.size());
                    if (pkh != TString::npos) {
                        auto pkf = data.find(PRIVKEY_FOOTER, pkh + PRIVKEY_HEADER.size());
                        if (pkf != TString::npos) {
                            cert.assign(data.substr(ch, cf - ch + CERT_FOOTER.size()));
                            privkey.assign(data.substr(pkh, pkf - pkh + PRIVKEY_FOOTER.size()));
                            return;
                        }
                    }
                }
            }
            ythrow yexception() << "invalid CA servert reply for " << host;
        }
    private:
        TSimpleHttpClient HttpClient;
};

