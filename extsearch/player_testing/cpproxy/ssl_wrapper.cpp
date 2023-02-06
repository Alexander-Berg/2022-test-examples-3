#include "ssl_wrapper.h"
#include <util/generic/yexception.h>
#include <util/string/cast.h>
#include <contrib/libs/openssl/include/openssl/bio.h>
#include <contrib/libs/openssl/include/openssl/evp.h>
#include <contrib/libs/openssl/include/openssl/x509.h>
#include <contrib/libs/openssl/include/openssl/crypto.h>
#include <contrib/libs/openssl/include/openssl/x509v3.h>
#include <contrib/libs/openssl/include/openssl/safestack.h>
#include <library/cpp/http/client/client.h>

namespace {
    ui32 SSL_CONNECTION_TIMEOUT = 30;
}

SSL_CTX* TSSLConnectionWrapper::ClientContext = nullptr;
SSL_CTX* TSSLConnectionWrapper::ServerContext = nullptr;

TSSLConnectionWrapper::TSSLConnectionWrapper(TLog& logger)
    : Logger(logger)
    , IsClient(false)
    , NoShutdown(false)
    , BytesReceived(0LL)
    , BytesSent(0LL)
    , RawStream(nullptr)
    {}

TSSLConnectionWrapper::~TSSLConnectionWrapper() {
    try {
        Close();
    } catch(...) {
    }
}

void TSSLConnectionWrapper::Init(const TString& caCertDir) {
    OPENSSL_init_ssl(OPENSSL_INIT_LOAD_SSL_STRINGS | OPENSSL_INIT_LOAD_CRYPTO_STRINGS, nullptr);
    SSL_load_error_strings();
    ERR_load_crypto_strings();

    Y_ENSURE(!ClientContext);
    ClientContext = SSL_CTX_new(TLS_client_method());
    if (!ClientContext) {
        ythrow yexception() << "failed to create client SSL context";
    }
    SSL_CTX_load_verify_locations(ClientContext, nullptr, caCertDir.data());
    X509_STORE_set_flags(SSL_CTX_get_cert_store(ClientContext), X509_V_FLAG_TRUSTED_FIRST);
    X509_STORE_set_flags(SSL_CTX_get_cert_store(ClientContext), X509_V_FLAG_PARTIAL_CHAIN);

    auto ctx_options = SSL_OP_ALL | SSL_OP_NO_SSLv2 | SSL_OP_NO_SSLv3 | SSL_OP_NO_COMPRESSION;
    SSL_CTX_set_options(ClientContext, ctx_options);
    SSL_CTX_set_verify(ClientContext, SSL_VERIFY_NONE, nullptr);
    SSL_CTX_set_cipher_list(ClientContext, nullptr);

    Y_ENSURE(!ServerContext);
    ServerContext = SSL_CTX_new(SSLv23_server_method());
    if (!ServerContext) {
        ythrow yexception() << "failed to create server SSL context";
    }
}

void TSSLConnectionWrapper::FakeInit(TStreamSocket* stream) {
    RawStream = stream;
}

void TSSLConnectionWrapper::ClientInit(const TString& host) {
    IsClient = true;
    Host = host;
}


static void LoadCertificate(SSL* ssl, const TString& certData, const TString& privkeyData) {
    THolder<BIO, TDestroySSL<BIO, BIO_vfree>> certBio(BIO_new(BIO_s_mem()));
    THolder<BIO, TDestroySSL<BIO, BIO_vfree>> pkBio(BIO_new(BIO_s_mem()));
    if (!certBio || !pkBio) {
        ythrow yexception() << "failed to create BIO";
    }
    BIO_write(certBio.Get(), certData.data(), certData.size());
    BIO_write(pkBio.Get(), privkeyData.data(), privkeyData.size());
    THolder<X509, TDestroySSL<X509, X509_free>> cert(PEM_read_bio_X509(certBio.Get(), nullptr, nullptr, nullptr));
    if (!cert) {
        ythrow yexception() << "failed to read X509 cert";
    }
    THolder<EVP_PKEY, TDestroySSL<EVP_PKEY, EVP_PKEY_free>> privkey(PEM_read_bio_PrivateKey(pkBio.Get(), nullptr, nullptr, nullptr));
    if (!privkey) {
        ythrow yexception() << "failed to read private key";
    }
    if (SSL_use_certificate(ssl, cert.Get()) != 1) {
        ythrow yexception() << "failed to use certificate";
    }
    if (SSL_use_PrivateKey(ssl, privkey.Get()) != 1) {
        ythrow yexception() << "failed to use private key";
    }
    if (SSL_check_private_key(ssl) != 1) {
        ythrow yexception() << "certificate/private key mismatch";
    }
}

void TSSLConnectionWrapper::ServerInit(const TString& certData, const TString& privkeyData) {
    CertData = certData;
    PrivkeyData = privkeyData;
    IsClient = false;
}

static bool VerifyDomainName(TStringBuf certName, TStringBuf hostName) {
    auto certNameBuf = certName;
    auto firstPart = certNameBuf.NextTok('.');
    if (firstPart == "*") {
        certName = certNameBuf;
        hostName.NextTok('.');
    }
    return certName.size() == hostName.size() && !strncasecmp(certName.data(), hostName.data(), hostName.size());
}

static TString GetIssuerCertURL(X509 *cert) {
    TString url;
    auto* info = reinterpret_cast<AUTHORITY_INFO_ACCESS *>(X509_get_ext_d2i(cert, NID_info_access, nullptr, nullptr));
    if (!info) {
        return url;
    }
    for (int i = 0; i < sk_ACCESS_DESCRIPTION_num(info); i++) {
        auto* ad = sk_ACCESS_DESCRIPTION_value(info, i);
        if (OBJ_obj2nid(ad->method) == NID_ad_ca_issuers && ad->location->type == GEN_URI) {
            url.assign((const char*)ASN1_STRING_get0_data(ad->location->d.uniformResourceIdentifier),
                          ASN1_STRING_length(ad->location->d.uniformResourceIdentifier));
            break;
        }
    }
    AUTHORITY_INFO_ACCESS_free(info);
    return url;
}

static void VerifyPeerCertificateDomain(X509* cert, const TString& host) {
    TString certName;
    /* verify altName */
    STACK_OF(GENERAL_NAME)* names = (STACK_OF(GENERAL_NAME)*)X509_get_ext_d2i(cert, NID_subject_alt_name, nullptr, nullptr);
    if (names) {
        int namesLen = sk_GENERAL_NAME_num(names);
        bool verifyOk = false;
        for (int i = 0; i < namesLen; ++i) {
            const GENERAL_NAME* name = sk_GENERAL_NAME_value(names, i);
            if (name->type == GEN_DNS) {
                TStringBuf dnsName((const char*)ASN1_STRING_get0_data(name->d.dNSName), ASN1_STRING_length(name->d.dNSName));
                verifyOk = VerifyDomainName(dnsName, host);
                if(verifyOk) {
                    break;
                }
                certName = ToString(dnsName);
            }
        }
        sk_GENERAL_NAME_pop_free(names, GENERAL_NAME_free);
        if (verifyOk) {
            return;
        }
    }
    /* verify commonName */
    int cnameLoc = X509_NAME_get_index_by_NID(X509_get_subject_name(cert), NID_commonName, -1);
    if (cnameLoc >= 0) {
        auto* cnameEntry = X509_NAME_get_entry(X509_get_subject_name(cert), cnameLoc);
        if (cnameEntry) {
            auto* cnameData = X509_NAME_ENTRY_get_data(cnameEntry);
            if (cnameData) {
                TStringBuf cname((const char*)ASN1_STRING_get0_data(cnameData), ASN1_STRING_length(cnameData));
                if (VerifyDomainName(cname, host)) {
                    return;
                }
                certName = ToString(cname);
            }
        }
    }
    ythrow yexception() << "can't match host certificate name (" << certName << ") with host name: " << host;
}

#if 0
static void VerifyPeerCertificateValidityPeriod(X509* cert, const TString& host) {
    auto notBefore = X509_get_notBefore(cert);
    auto notAfter = X509_get_notAfter(cert);
    THolder<ASN1_TIME, TDestroySSL<ASN1_TIME, ASN1_TIME_free>> now(ASN1_TIME_set(nullptr, TInstant::Now().TimeT()));
    int daysBefore, secondsBefore;
    Y_ENSURE(ASN1_TIME_diff(&daysBefore, &secondsBefore, notBefore, now.Get()) == 1);
    int daysAfter, secondsAfter;
    Y_ENSURE(ASN1_TIME_diff(&daysAfter, &secondsAfter, now.Get(), notAfter) == 1);
    if (daysBefore * 86400 + secondsBefore < 0 || daysAfter * 86400 + secondsAfter <= 0) {
        ythrow yexception() << "host " << host << " certificate not valid, before: " << daysBefore << "d, after: " << daysAfter << "d";
    }
}
#endif

static X509* FetchCertificateByURL(const TString& url) {
    try {
        auto resp = NHttp::Fetch(url);
        if (!resp->Success() || resp->Code != 200) {
            return nullptr;
        } else {
            THolder<BIO, TDestroySSL<BIO, BIO_vfree>> certBio(BIO_new(BIO_s_mem()));
            if (!certBio) {
                return nullptr;
            }
            BIO_write(certBio.Get(), resp->Data.data(), resp->Data.size());
            return d2i_X509_bio(certBio.Get(), nullptr);
        }
    } catch(...) {
        return nullptr;
    }
}

static bool VerifyIncompleteChain(TLog& logger, SSL* ssl, X509* cert, STACK_OF(X509)* chain, const TString& host) {
    logger << TLOG_DEBUG << "host " << host << " has incomplete certificate chain";
    auto* other = sk_X509_new_null();
    for (int i = 0; i < sk_X509_num(chain); i++) {
        X509* item = sk_X509_value(chain, i);
        TString url = GetIssuerCertURL(item);
        if (url) {
            auto* issuerCert = FetchCertificateByURL(url);
            if (issuerCert) {
                logger << TLOG_DEBUG << "#" << i << ": issuer cert fetched from " << url;
                sk_X509_push(other, issuerCert);
            }
        }
    }
    auto* storeCtx = X509_STORE_CTX_new();
    X509_STORE_CTX_init(storeCtx, SSL_CTX_get_cert_store(SSL_get_SSL_CTX(ssl)), cert, other);
    auto verifyResult = X509_verify_cert(storeCtx);
    logger << TLOG_DEBUG << "second attempt verifyResult: " << verifyResult << ", error: " << X509_verify_cert_error_string(X509_STORE_CTX_get_error(storeCtx));
    for (int i = 0; i < sk_X509_num(other); i++) {
        X509_free(sk_X509_value(other, i));
    }
    sk_X509_free(other);
    X509_STORE_CTX_free(storeCtx);
    return verifyResult == 1;
}

static void VerifyPeerCertificate(TLog& logger, SSL* ssl, X509* cert, const TString& host) {
    auto res = SSL_get_verify_result(ssl);
    if (res != X509_V_OK) {
        bool dontFail = false;
        if (res == X509_V_ERR_UNABLE_TO_GET_ISSUER_CERT_LOCALLY) {
            STACK_OF(X509)* chain = SSL_get_peer_cert_chain(ssl);
            if (chain) {
                dontFail = VerifyIncompleteChain(logger, ssl, cert, chain, host);
            }
        }
        if (!dontFail)  {
            ythrow yexception() << "certificate verification failed: " << X509_verify_cert_error_string(res) << " (" << res << ")";
        }
    }
    VerifyPeerCertificateDomain(cert, host);
#if 0
    /* not required if SSL_VERIFY_PEER is set */
    VerifyPeerCertificateValidityPeriod(cert, host);
#endif
}

void TSSLConnectionWrapper::Connect(SOCKET fd) {
    if (RawStream) {
        return;
    }
    Y_ENSURE(!!ClientContext && !Connection && fd >= 0);
    SetSocketTimeout(fd, SSL_CONNECTION_TIMEOUT);
    Connection.Reset(SSL_new(ClientContext));
    if (Host) {
        SSL_set_tlsext_host_name(Connection.Get(), Host.data());
    }
    if (!SSL_set_fd(Connection.Get(), fd)) {
        ythrow yexception() << "failed to setup SSL connection socket";
    }
    auto ret = SSL_connect(Connection.Get());
    if (ret <= 0) {
        auto error = SSL_get_error(Connection.Get(), ret);
        char errBuf[4096];
        ERR_error_string_n(error, errBuf, sizeof(errBuf));
        ythrow yexception() << "SSL_connect failed: " << errBuf << " (" << error << "," << ret << ")";
    }
    THolder<X509, TDestroySSL<X509, X509_free>> peerCert(SSL_get_peer_certificate(Connection.Get()));
    if (!peerCert) {
        ythrow yexception() << "SSL_get_peer_certificate: no certificate returned";
    }
    VerifyPeerCertificate(Logger, Connection.Get(), peerCert.Get(), Host);
    NoShutdown = false;
}

void TSSLConnectionWrapper::Accept(SOCKET fd) {
    if (RawStream) {
        return;
    }
    Y_ENSURE(!!ServerContext && !Connection && fd >= 0);
    Connection.Reset(SSL_new(ServerContext));
    LoadCertificate(Connection.Get(), CertData, PrivkeyData);
    if (!SSL_set_fd(Connection.Get(), fd)) {
        ythrow yexception() << "failed to setup SSL connection socket";
    }
    auto ret = SSL_accept(Connection.Get());
    if (ret <= 0) {
        auto error = SSL_get_error(Connection.Get(), ret);
        ythrow yexception() << "SSL_accept failed: " << error;
    }
    NoShutdown = false;
}

ssize_t TSSLConnectionWrapper::Recv(void* data, size_t size) {
    if (RawStream) {
        return RawStream->Recv(data, size);
    }
    Y_ENSURE(!!Connection);
    auto n_tries = 32;
    auto ret = 0;
    for (auto i = 0; i < n_tries; ++i) {
        ret = SSL_read(Connection.Get(), data, size);
        if (ret > 0) {
            break;
        }
        auto error = SSL_get_error(Connection.Get(), ret);
        if (error == SSL_ERROR_WANT_READ || error == SSL_ERROR_WANT_WRITE) {
            continue;
        } else if (error == SSL_ERROR_ZERO_RETURN) {
            return 0;
        } else if (error == SSL_ERROR_SYSCALL || error == SSL_ERROR_SSL) {
            NoShutdown = true;
        }
        char errBuf[4096];
        ERR_error_string_n(error, errBuf, sizeof(errBuf));
        Logger << TLOG_DEBUG << "SSL_read failed: " << errBuf << " (" << error << "), r:" << BytesReceived << " s:" << BytesSent;
        return 0;
    }
    BytesReceived += ret;
    return ret;
}

ssize_t TSSLConnectionWrapper::Send(const void* data, size_t size) {
    if (RawStream) {
        return RawStream->Send(data, size);
    }
    Y_ENSURE(!!Connection);
    auto n_tries = 32;
    auto ret = 0;
    for (auto i = 0; i < n_tries; ++i) {
        ret = SSL_write(Connection.Get(), data, size);
        if (ret > 0) {
            break;
        }
        auto error = SSL_get_error(Connection.Get(), ret);
        if (error == SSL_ERROR_WANT_READ || error == SSL_ERROR_WANT_WRITE) {
            continue;
        } else if (error == SSL_ERROR_ZERO_RETURN) {
            return 0;
        } else if (error == SSL_ERROR_SYSCALL || error == SSL_ERROR_SSL) {
            NoShutdown = true;
        }
        char errBuf[4096];
        ERR_error_string_n(error, errBuf, sizeof(errBuf));
        Logger << TLOG_DEBUG << "SSL_write failed: " << errBuf << " (" << error << "), r:" << BytesReceived << " s:" << BytesSent;
        return -1;
    }
    BytesSent += ret;
    return ret;
}

bool TSSLConnectionWrapper::HasIncomingData() {
    if (RawStream) {
        return false;
    }
    Y_ENSURE(!!Connection);
    return SSL_pending(Connection.Get());
}

void TSSLConnectionWrapper::Close() {
    if (!!Connection) {
        if (!NoShutdown) {
            SSL_shutdown(Connection.Get());
        }
        Connection.Destroy();
    }
    RawStream = nullptr;
}

