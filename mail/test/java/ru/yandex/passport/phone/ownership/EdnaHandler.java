package ru.yandex.passport.phone.ownership;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ru.yandex.http.util.BadRequestException;
import ru.yandex.passport.phone.ownership.edna.AddressRequestStatus;

public class EdnaHandler implements HttpRequestHandler {
    private final HashMap<String, String> imsiByPhone = new HashMap<>();
    private final HashSet<String> subsribers = new HashSet<>();
    private final String login;
    private final String password;

    public EdnaHandler(String login, String password) {
        this.login = login;
        this.password = password;
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws BadRequestException {
        try {
            if (!(httpRequest instanceof HttpEntityEnclosingRequest)) {
                throw new BadRequestException("Payload expected");
            }
            HttpEntity requestEntity = ((HttpEntityEnclosingRequest) httpRequest).getEntity();
            Document requestDoc = DocumentParser.entityToDocument(requestEntity);
            System.out.println("request to edna: " + DocumentParser.documentToString(requestDoc));
            if (!checkAuth(requestDoc)) {
                throw new BadRequestException("Authentication failed");
            }
            String request = requestDoc.getDocumentElement().getNodeName();
            NodeList adresses = requestDoc.getElementsByTagName("address");

            Document responseDoc = null;
            switch (request) {
                case "provideSubscriberImsiRequest":
                    responseDoc = constructResponse(adresses, AddressRequestStatus.OK, request);
                    break;
                case "unsubscribeSubscriberRequest":
                    responseDoc = constructResponse(adresses, AddressRequestStatus.OK, request);
                    break;
                default:
                    throw new BadRequestException("Unknown request: " + request);
            }
            System.out.println("response prepared" + DocumentParser.documentToString(responseDoc));
            HttpEntity entity = DocumentParser.documentToStringEntity(responseDoc);
            httpResponse.setEntity(entity);
        } catch (ParserConfigurationException | SAXException | TransformerException e) {
            throw new BadRequestException("Error occured while constructihg the response");
        } catch (IOException e) {
            throw new BadRequestException("Error occured while the payload parsing");
        }
    }

    private boolean checkAuth(Document requestDoc) {
        Node header = requestDoc.getElementsByTagName("header").item(0);
        if (header == null) {
            return false;
        }
        Element headerElement = (Element) header;
        String login = headerElement.getElementsByTagName("login").item(0).getTextContent();
        String password = headerElement.getElementsByTagName("password").item(0).getTextContent();
        return (login.equals(this.login) && password.equals(this.password));
    }

    private Document constructResponse(NodeList adresses, String statusCode, String request)
        throws ParserConfigurationException, BadRequestException {
        DocumentBuilder documentBuilder =
            DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();
        Document doc = documentBuilder.newDocument();
        Element rootElement = doc.createElement(request);
        Element header = doc.createElement("header");
        Element payload = doc.createElement("payload");
        Element subscriberAddressList = doc.createElement("subscriberAddressList");
        for (int i = 0; i < adresses.getLength(); i++) {
            Node address = adresses.item(i);
            String phone = address.getTextContent();
            addSubsriberAddress(subscriberAddressList, phone, request);
        }
        Element code = doc.createElement("code");
        code.setTextContent(statusCode);
        payload.appendChild(code);
        payload.appendChild(subscriberAddressList);
        rootElement.appendChild(header);
        rootElement.appendChild(payload);
        doc.appendChild(rootElement);
        return doc;
    }

    private void addSubsriberAddress(Element subscriberAddressList, String phone, String request) throws BadRequestException {
        Document doc = subscriberAddressList.getOwnerDocument();

        Element subscriberAddress = doc.createElement("subscriberAddress");
        Element address = doc.createElement("address");
        address.setTextContent(phone);
        subscriberAddress.appendChild(address);
        fillSubsriberAddress(subscriberAddress, phone, request);
        subscriberAddressList.appendChild(subscriberAddress);
    }

    private void fillSubsriberAddress(Element subscriberAddress, String phone, String request) throws BadRequestException {
        switch (request) {
            case "provideSubscriberImsiRequest":
                if (imsiByPhone.containsKey(phone)) {
                    fillImsiSubsriberAddress(subscriberAddress, phone);
                } else {
                    fillImsiSubsriberAddress(subscriberAddress, phone);
                }
                break;
            case "unsubscribeSubscriberRequest":
                fillUnsubsribeSubscriberAddress(subscriberAddress, phone);
                break;
            default:
                throw new BadRequestException("Unknown request: " + request);
        }
    }

    private void fillUnsubsribeSubscriberAddress(Element subscriberAddress, String phone) {
        Document doc = subscriberAddress.getOwnerDocument();
        String addressCode = subsribers.contains(phone) ? AddressRequestStatus.OK :
            AddressRequestStatus.ERROR_NOT_SUBSRIBED;
        Element code = doc.createElement("code");
        code.setTextContent(addressCode);
        subscriberAddress.appendChild(code);
        System.out.println("unsubscribe " + phone);
        unsubscribe(phone);
    }

    private void fillImsiSubsriberAddress(Element subscriberAddress, String phone) {
        Document doc = subscriberAddress.getOwnerDocument();
        boolean phoneExists = imsiByPhone.containsKey(phone);
        String addressCode = phoneExists ? AddressRequestStatus.OK : AddressRequestStatus.ERROR_ADDRESS_UNKNOWN;
        Element code = doc.createElement("code");
        code.setTextContent(addressCode);
        subscriberAddress.appendChild(code);
        if (phoneExists) {
            String imsiValue = imsiByPhone.get(phone);
            Element subscriberImsi = doc.createElement("subscriberImsi");

            Element imsi = doc.createElement("imsi");
            imsi.setTextContent(imsiValue);

            subscriberImsi.appendChild(imsi);
            subscriberAddress.appendChild(subscriberImsi);
            subscribe(phone);
        }
    }

    public void addPhone(String phone, String imsi) {
        imsiByPhone.put(phone, imsi);
    }

    public void removePhone(String phone) {
        imsiByPhone.remove(phone);
    }

    public void subscribe(String phone) {
        subsribers.add(phone);
    }

    public void unsubscribe(String phone) {
        subsribers.remove(phone);
    }

    public boolean hasSubscriber(String phone) {
        return subsribers.contains(phone);
    }
}
