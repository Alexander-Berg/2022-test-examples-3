package ru.yandex.direct.balance.client;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcHttpTransportException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.direct.balance.client.exception.BalanceClientException;

import static org.assertj.core.api.Assertions.assertThat;

// тесты на восприятие ошибок баланса. тексты приведены не полностью.
class GuessStatusByExceptionTest {

    @ParameterizedTest(name = "{arguments}")
    @ValueSource(strings = {
            "Error: ProtocolError\nDescription: " +
                    "<ProtocolError for balance-payments.paysys.yandex.net:8023/simpleapi/xmlrpc: 502 Bad Gateway>",
            "Error: DatabaseError\nDescription: (cx_Oracle.DatabaseError) " +
                    "ORA-25408: can not safely replay call",
            "Error: DatabaseError\nDescription: (cx_Oracle.DatabaseError) " +
                    "ORA-12514: TNS:listener does not currently know of service requested in connect descriptor",
            "Error: HTTPError\nDescription: 504 Server Error: Gateway Time-out for url: " +
                    "https://trust-payments.paysys.yandex.net:8028/trust-payments/v2/bindings/",
            "Error: OperationalError\nDescription: (cx_Oracle.OperationalError) " +
                    "ORA-03135: connection lost contact\nProcess ID: 0\nSession ID: 0 Serial number: 0",
            "Error: OperationalError\nDescription: (cx_Oracle.OperationalError) " +
                    "ORA-03113: end-of-file on communication channel\n" +
                    "Process ID: 39837\nSession ID: 13424 Serial number: 5089",
            "Error: OperationalError\nDescription: (cx_Oracle.OperationalError) " +
                    "ORA-00603: ORACLE server session terminated by fatal error\n" +
                    "Process ID: 154951\nSession ID: 7844 Serial number: 16972",
            "Error: ConnectionError\nDescription: " +
                    "HTTPSConnectionPool(host='trust-payments.paysys.yandex.net', port=8028): " +
                    "Max retries exceeded with url: /trust-payments/v2/payment_methods?show_enabled=1&show_bound",
            "Error: AttributeError\nDescription: 'Client' object has no attribute 'session_passport'",
            "Error: AttributeError\nDescription: type object 'PermissionCode' has no attribute 'PERSON_EXT_EDIT'",
            "Error: error\nDescription: [Errno 111] Connection refused",
            "Error: error\nDescription: [Errno 113] No route to host",
            "Error: SSLError\nDescription: bad handshake: SysCallError(104, 'Connection reset by peer')",
            "Error: Terminated\nDescription: \n",

            "Failed to read server's response: Network is unreachable",
            "Failed to read server's response: No route to host (Host unreachable)",
            "Failed to read server's response: readHandshakeRecord",
            "Failed to read server's response: Remote host terminated the handshake",
            "Failed to read server's response: Couldn't kickstart handshaking",
            "Failed to read server's response: connect timed out",
            "Failed to read server's response: Connection timed out",
            "Failed to read server's response: Read timed out",
            "Failed to read server's response: Connection reset",
            "Failed to read server's response: Connection refused",
            "Failed to read server's response: balance-xmlrpc.yandex.net", // ??
            "Failed to read server's response: balance-simpleapi.yandex.ru", // ??
            "Failed to parse server's response: Content is not allowed in prolog.",
            "Failed to create input stream: connect timed out",
            "Failed to create input stream: Read timed",
            "Failed to create input stream: Connection timed out",
            "Failed to create input stream: Connection reset",
            "Failed to create input stream: Connection refused (Connection refused)",

            "<error><msg>Error in trust api call: RuntimeError: HttpError: 504 Gateway Time-out</msg>" +
                    "<wo-rollback>0</wo-rollback>" +
                    "<trust-error>RuntimeError: HttpError: 504 Gateway Time-out</trust-error><method>",
            "<error><msg>Error in trust api call: Server internal error</msg><wo-rollback>0</wo-rollback>" +
                    "<trust-error>Server internal error</trust-error>" +
                    "<method>Balance2.PayRequest</method>" +
                    "<code>TRUST_API_EXCEPTION</code>",
            "<error><msg>Error in balance payments api call: timeout during read(5643) on wsgi.input</msg>" +
                    "<wo-rollback>0</wo-rollback><trust-error>timeout during read(5643) on wsgi.input</trust-error>" +
                    "<method>Balance2.PayRequest</method><code>BALANCE_PAYMENTS_EXCEPTION</code>",
            "<error><msg>Error in balance payments api call: [Errno 11] Resource temporarily unavailable</msg>" +
                    "<wo-rollback>0</wo-rollback>" +
                    "<trust-error>[Errno 11] Resource temporarily unavailable</trust-error>" +
                    "<method>Balance2.PayRequest</method><code>BALANCE_PAYMENTS_EXCEPTION</code>",
            "<error><msg>Error in balance payments api call: " +
                    "SSLError: HTTPSConnectionPool(host='trust-payments.paysys.yandex.net', port=8028): " +
                    "Max retries exceeded with url: /trust-payments/v2/orders " +
                    "(Caused by SSLError(SSLError(\"bad handshake: SysCallError(-1, 'Unexpected EOF')\",),))" +
                    "</msg><wo-rollback>0</wo-rollback>",
            "<error><msg>Error in balance payments api call: " +
                    "IOError: [Errno 11] Resource temporarily unavailable</msg><wo-rollback>0</wo-rollback>" +
                    "<trust-error>IOError: [Errno 11] Resource temporarily unavailable</trust-error>" +
                    "<method>Balance2.PayRequest</method><code>BALANCE_PAYMENTS_EXCEPTION</code>",
            "<error><msg>Error in balance payments api call: " +
                    "HTTPError: 504 Server Error: Gateway Time-out for url: " +
                    "https://trust-payments.paysys.yandex.net:8028/trust-payments/v2/orders/111878294-272715009</msg>" +
                    "<wo-rollback>0</wo-rollback>",
            "<error><msg>Error in balance payments api call: " +
                    "HTTPError: 502 Server Error: Bad Gateway for url: " +
                    "https://trust-payments.paysys.yandex.net:8028/trust-payments/v2/orders/111491759-1043956206" +
                    "</msg><wo-rollback>0</wo-rollback>",
            "<error><msg>Error in balance payments api call: HTTPError: " +
                    "500 Server Error: Internal Server Error for url: " +
                    "https://trust-payments.paysys.yandex.net:8028/trust-payments/v2/payments</msg>" +
                    "<wo-rollback>0</wo-rollback>",

            // вот это спорная ошибка - непонятно что там под капотом, пока зачислим в 5ки.
            // если будет false positive - нужно вместе с балансом и трастом разбираться, что тут реально происходит
            "<error><msg>Error in balance payments api call: HTTPError: 400 Client Error: Bad Request for url: " +
                    "https://trust-payments.paysys.yandex.net:8028/trust-payments/v2/payments</msg>" +
                    "<wo-rollback>0</wo-rollback>",

            "<error><msg>Error in balance payments api call: DatabaseError: (cx_Oracle.DatabaseError) " +
                    "ORA-12520: TNS:listener could not find available handler for requested type of server",
            "<error><msg>Error in balance payments api call: DatabaseError: (cx_Oracle.DatabaseError) " +
                    "ORA-12518: TNS:listener could not hand off client connection",
            "<error><msg>Error in balance payments api call: DatabaseError: (cx_Oracle.DatabaseError) " +
                    "ORA-12514: TNS:listener does not currently know of service requested in connect descriptor",
            "<error><msg>Error in balance payments api call: ConnectionError: " +
                    "HTTPSConnectionPool(host='trust-payments.paysys.yandex.net', port=8028): " +
                    "Max retries exceeded with url: /trust-payments/v2/orders/111878653-574187965 " +
                    "(Caused by NewConnectionError(&lt;urllib3.connection.VerifiedHTTPSConnection object",

            "<error><msg>Request 3354543478 is locked by another operation</msg><wo-rollback>0</wo-rollback>" +
                    "<request-id>3354543478</request-id>" +
                    "<method>Balance2.PayRequest</method>" +
                    "<code>REQUEST_IS_LOCKED</code><parent-codes><code>EXCEPTION</code></parent-codes>",
    })
    void resolvedAsServerError(String message) {
        Exception e = new XmlRpcException(message);
        var status = BalanceXmlRpcClient.guessStatusByException(e);
        assertThat(status.getName()).isEqualTo("server_error");
    }


    @ParameterizedTest(name = "{arguments}")
    @ValueSource(strings = {
            "<error><msg>Error in trust api call: User 447183678 has too many active bindings (7)</msg>" +
                    "<wo-rollback>0</wo-rollback>" +
                    "<trust-error>User 447183678 has too many active bindings (7)</trust-error>",
            "<error><msg>Error in trust api call: User 900780820 has too many active bindings (9)</msg>" +
                    "<wo-rollback>0</wo-rollback>" +
                    "<trust-error>User 900780820 has too many active bindings (9)</trust-error>" +
                    "<method>Balance2.GetCardBindingURL</method>" +
                    "<code>TRUST_API_EXCEPTION</code>",
            "<error><msg>User 755009055 has no permission IssueInvoices.</msg>" +
                    "<user-id>755009055</user-id><wo-rollback>0</wo-rollback>" +
                    "<perm>IssueInvoices</perm>" +
                    "<method>Balance2.PayRequest</method>" +
                    "<code>PERMISSION_DENIED</code><parent-codes><code>EXCEPTION</code></parent-codes>",
            "<error><msg>QTY for order_id = 1909893714 do not match " +
                    "(balance value=618799.970000, service value=618799.980000</msg>" +
                    "<balance-consume-qty /><order-id>1909893714</order-id><wo-rollback>0</wo-rollback>" +
                    "<service-consume-qty>618799.980000</service-consume-qty>" +
                    "<method>Balance2.CreateTransferMultiple</method>" +
                    "<code>ORDERS_NOT_SYNCHRONIZED</code>" +
                    "<parent-codes><code>EXCEPTION</code></parent-codes>",
            "<error><msg>PromoCode: Can't tear promocode off: PC_TEAR_OFF_NO_FREE_CONSUMES</msg>" +
                    "<wo-rollback>0</wo-rollback><tanker-key>PC_TEAR_OFF_NO_FREE_CONSUMES</tanker-key>" +
                    "<method>Balance2.TearOffPromocode</method>" +
                    "<code>CANT_TEAR_PC_OFF</code><parent-codes><code>EXCEPTION</code></parent-codes>",
            "<error><msg>Invalid parameter for function: You should provide SOME filtering criteria</msg>" +
                    "<wo-rollback>0</wo-rollback>" +
                    "<method>Balance2.FindClient</method><code>INVALID_PARAM</code>",
            "<error><msg>Invalid parameter for function: No payment options available</msg>" +
                    "<wo-rollback>0</wo-rollback><method>Balance2.PayRequest</method><code>INVALID_PARAM</code>",
            "<error><msg>Invalid parameter for function: " +
                    "New migrate_to_currency must be not less than 2020-06-17 10:55:59.087950</msg>" +
                    "<wo-rollback>0</wo-rollback><method>Balance2.CreateClient</method><code>INVALID_PARAM</code>",
            "<error><msg>Cannot create person with type = ph, available types: sw_yt, sw_ytph, ytph, by_ytph</msg>" +
                    "<person-type>ph</person-type><tanker-fields>['person_type']</tanker-fields>" +
                    "<wo-rollback>0</wo-rollback><method>Balance2.CreatePerson</method>" +
                    "<code>INVALID_PERSON_TYPE</code>",
    })
    void resolvedAsClientError(String message) {
        Exception e = new XmlRpcException(message);
        var status = BalanceXmlRpcClient.guessStatusByException(e);
        assertThat(status.getName()).isEqualTo("client_error");
    }

    @Test
    void resolvedAsUnparsed() {
        Exception e = new BalanceClientException("test");
        var status = BalanceXmlRpcClient.guessStatusByException(e);
        assertThat(status.getName()).isEqualTo("unparsable");
    }

    @Test
    void resolvedAsUnknown() {
        Exception e = new NumberFormatException("test");
        var status = BalanceXmlRpcClient.guessStatusByException(e);
        assertThat(status.getName()).isEqualTo("unknown");
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 302, 301, 102})
    void otherHttpResolvedAsUnknown() {
        Exception e = new XmlRpcHttpTransportException(302, "test");
        var status = BalanceXmlRpcClient.guessStatusByException(e);
        assertThat(status.getName()).isEqualTo("unknown");
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 404})
    void http4xxResolvedAsClientError(int code) {
        Exception e = new XmlRpcHttpTransportException(code, "test");
        var status = BalanceXmlRpcClient.guessStatusByException(e);
        assertThat(status.getName()).isEqualTo("client_error");
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503, 504})
    void http5xxResolvedAsServerError(int code) {
        Exception e = new XmlRpcHttpTransportException(code, "test");
        var status = BalanceXmlRpcClient.guessStatusByException(e);
        assertThat(status.getName()).isEqualTo("server_error");
    }
}
