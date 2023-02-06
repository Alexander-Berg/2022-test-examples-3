package ru.yandex.market.logistics.yard_v2.external.pass_connector.sofyino

import org.apache.http.Consts
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.xml.sax.InputSource
import ru.yandex.market.logistics.yard_v2.config.gozora.GoZoraProxy
import ru.yandex.market.logistics.yard_v2.external.pass_connector.sofyino_test.config.ClientTestConfig
import java.io.StringReader
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory


@Service
class SofyinoSoapClientTest(
    val clientConfig: ClientTestConfig,
    val goZoraProxy: GoZoraProxy
) {

    fun issue(licensePlate: String, date: String): String? {
        val httpPost = clientConfig.getHttpPost()

        val body = getIssueCarRequestBody(licensePlate, date)
        httpPost.entity = StringEntity(body, Consts.UTF_8)


        val responseBody = executeSoapPost(httpPost, clientConfig.getSofyinoClientConfig())
            ?: throw RuntimeException("Empty response body from Sofyino")
        return extractExternalCode(responseBody)
    }

    fun issueForCourier(name: String, date: String): String? {
        val split = name.split(' ')

        val lastName = split.getOrElse(0) { NO_INFO_SIGN }
        val firstName = split.getOrElse(1) { NO_INFO_SIGN }
        val secondName = split.getOrElse(2) { NO_INFO_SIGN }

        val httpPost = clientConfig.getHttpPost()

        val body = getIssueCourierRequestBody(firstName, lastName, secondName, date)
        httpPost.entity = StringEntity(body, Consts.UTF_8)

        val responseBody = executeSoapPost(httpPost, clientConfig.getSofyinoClientConfig())
            ?: throw RuntimeException("Empty response body from Sofyino")
        return extractExternalCode(responseBody)
    }

    private fun extractExternalCode(responseBody: String): String? {
        val db: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val inputSource = InputSource(StringReader(responseBody))

        return db.parse(inputSource).getElementsByTagName("m:return").item(0).firstChild.nodeValue
    }

    private fun getIssueCarRequestBody(licensePlate: String, date: String): String {
        return """<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:test="test">
        <soap:Header/>
        <soap:Body>
        <test:yandex>
        <test:Data>$licensePlate</test:Data>
        <test:number>$date</test:number>
        </test:yandex>
        </soap:Body>
        </soap:Envelope>
        """
    }

    private fun getIssueCourierRequestBody(firstname: String, secondName: String, lastName: String, date: String): String {
        return """<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:test="test">
        <soap:Header/>
        <soap:Body>
        <test:yandexhuman>
        <test:Data>$firstname</test:Data>
        <test:otchestvo>$date</test:otchestvo>
        <test:familia>$secondName</test:familia>
        <test:name>$lastName</test:name>
        </test:yandexhuman>
        </soap:Body>
        </soap:Envelope>
        """
    }

    private fun executeSoapPost(httpPost: HttpPost, config: RequestConfig): String? {
        val postRequestThroughProxy = goZoraProxy.getPostRequestThroughProxy(httpPost)

        val httpClient = HttpClients.custom()
            .setDefaultRequestConfig(config)
            .disableAutomaticRetries()
            .build()

        httpClient.use {
            log.info("Try to execute http post trough GoZora proxy {}.", postRequestThroughProxy)
            val response = it.execute(postRequestThroughProxy)

            response.use { rsp ->
                val body = EntityUtils.toString(response.entity)
                log.info("Request: [{}]. Inner request: [{}]. Response: [{}]. Response body: [{}].",
                    getRequestExtendedInfo(postRequestThroughProxy), postRequestThroughProxy?.entity, rsp, body)
                return body
            }
        }
    }

    private fun getRequestExtendedInfo(request: HttpPost?): String {
        return if (request == null) ""
        else
            """${request.method} ${request.uri} ${request.protocolVersion} [${request.allHeaders?.contentToString()}]"""
    }

    companion object {
        private const val NO_INFO_SIGN = "-"

        val log: Logger = LoggerFactory.getLogger(SofyinoSoapClientTest::class.java)
    }
}
