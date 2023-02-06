package ru.yandex.market.mbi.api.controller.builders;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author jkt on 04.04.17.
 */
public class XmlBindings {

    private static final String CUTOFFS = "cutoffs";

    private static final String CUTOFF = "cutoff";
    private static final String SHOP_ID = "shop-id";
    private static final String TYPE = "type";
    private static final String UID = "uid";
    private static final String TID = "tid";
    private static final String NEED_TESTING = "need-testing";
    private static final String COMMENT = "comment";

    private static final String CONTEXT_CUTTOFF = "context-cuttoff";
    private static final String FROM_DATE = "from-date";
    private static final String CUTOFF_TYPE = "cutoff-type";

    private static final String CUTOFF_RESPONSES = "cutoff-responses";

    private static final String CUTOFF_RESPONSE = "cutoff-response";
    private static final String STATUS = "status";
    private static final String NOTIFICATION_STATUS = "notification-status";
    private static final String NEED_TESTING_STATUS = "need-testing-status";


    static <T> String marshall(T object) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(object.getClass());
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            StringWriter writer = new StringWriter();
            jaxbMarshaller.marshal(object, writer);

            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error marshalling %s to xml string", object), e);

        }
    }

    static <T> T unmarshall(String input, Class<T> targetClass) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(targetClass);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            Object unmarshallingResult = jaxbUnmarshaller.unmarshal(new StringReader(input));

            return targetClass.cast(unmarshallingResult);

        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Error unmarshalling %s from xml string", targetClass.getSimpleName()), e
            );
        }
    }


    @XmlRootElement(name = CUTOFFS)
    static class Cutoffs {

        private List<Cutoff> cutoffs = new ArrayList<>();

        public List<Cutoff> getCutoffs() {
            return cutoffs;
        }

        @XmlElement(name = CUTOFF)
        public void setCutoffs(List<Cutoff> cutoffs) {
            this.cutoffs = cutoffs;
        }
    }


    @XmlRootElement(name = CUTOFF)
    static class Cutoff extends AbstractDataHolder {

        public String getShopId() {
            return getStringData(SHOP_ID);
        }

        @XmlAttribute(name = SHOP_ID)
        public void setShopId(String shopId) {
            setData(SHOP_ID, shopId);
        }

        public String getType() {
            return getStringData(TYPE);
        }

        @XmlAttribute(name = TYPE)
        public void setType(String type) {
            setData(TYPE, type);
        }

        public String getUid() {
            return getStringData(UID);
        }

        @XmlAttribute(name = UID)
        public void setUid(String uid) {
            setData(UID, uid);
        }

        public String getTid() {
            return getStringData(TID);
        }

        @XmlAttribute(name = TID)
        public void setTid(String tid) {
            setData(TID, tid);
        }

        public Boolean getNeedTesting() {
            return (Boolean) getData(NEED_TESTING);
        }

        @XmlAttribute(name = NEED_TESTING)
        public void setNeedTesting(Boolean needTesting) {
            setData(NEED_TESTING, needTesting);
        }

        public String getComment() {
            return getStringData(COMMENT);
        }

        @XmlAttribute(name = COMMENT)
        public void setComment(String comment) {
            setData(COMMENT, comment);
        }

        public ContextCutoff getContextCutoff() {
            return (ContextCutoff) getData(CONTEXT_CUTTOFF);
        }

        @XmlElement(name = CONTEXT_CUTTOFF)
        public void setContextCutoff(ContextCutoff contextCutoff) {
            setData(CONTEXT_CUTTOFF, contextCutoff);
        }

    }

    @XmlRootElement(name = CONTEXT_CUTTOFF)
    static class ContextCutoff extends AbstractDataHolder {


        public String getFromDate() {
            return getStringData(FROM_DATE);
        }

        @XmlAttribute(name = FROM_DATE)
        public void setFromDate(String fromDate) {
            setData(FROM_DATE, fromDate);
        }

        public String getCutoffType() {
            return getStringData(CUTOFF_TYPE);
        }

        @XmlAttribute(name = CUTOFF_TYPE)
        public void setCutoffType(Integer cutoffType) {
            setData(CUTOFF_TYPE, cutoffType);
        }

        public String toString() {
            return description();
        }
    }

    @XmlRootElement(name = CUTOFF_RESPONSES)
    static class CutoffResponses {

        private List<CutoffResponse> cutoffResponses = new ArrayList<>();

        public List<CutoffResponse> getCutoffResponses() {
            return cutoffResponses;
        }

        @XmlElement(name = CUTOFF_RESPONSE)
        public void setCutoffResponses(List<CutoffResponse> cutoffs) {
            this.cutoffResponses = cutoffs;
        }
    }

    @XmlRootElement(name = CUTOFF_RESPONSE)
    static class CutoffResponse extends AbstractDataHolder {


        public String getShopId() {
            return getStringData(SHOP_ID);
        }

        @XmlAttribute(name = SHOP_ID)
        public void setShopId(String shopId) {
            setData(SHOP_ID, shopId);
        }

        public String getType() {
            return getStringData(TYPE);
        }

        @XmlAttribute(name = TYPE)
        public void setType(String type) {
            setData(TYPE, type);
        }

        public String getStatus() {
            return getStringData(STATUS);
        }

        @XmlAttribute(name = STATUS)
        public void setStatus(String status) {
            setData(STATUS, status);
        }

        public String getNotificationStatus() {
            return getStringData(NOTIFICATION_STATUS);
        }

        @XmlAttribute(name = NOTIFICATION_STATUS)
        public void setNotificationStatus(String notificationStatus) {
            setData(NOTIFICATION_STATUS, notificationStatus);
        }

        public String getNeedTestingStatus() {
            return getStringData(NEED_TESTING_STATUS);
        }

        @XmlAttribute(name = NEED_TESTING_STATUS)
        public void setNeedTestingStatus(String needTestingStatus) {
            setData(NEED_TESTING_STATUS, needTestingStatus);
        }

    }

    static class AbstractDataHolder {

        private static final String NOT_SET_FOR_TEST = "Not set for test";


        private Map<String, Object> data = new HashMap<>();

        private Map<String, Object> additionallySpecifiedFields = new HashMap<>();

        protected void setData(String key, Object value) {
            if (value == null) {
                data.remove(key);
                additionallySpecifiedFields.put(key, NOT_SET_FOR_TEST);
                return;
            }
            data.put(key, value);
            additionallySpecifiedFields.put(key, value);
        }

        protected String getStringData(String key) {
            return (String) data.get(key);
        }

        protected Object getData(String key) {
            return data.get(key);
        }


        void addDataFrom(AbstractDataHolder dataHolder) {
            if (dataHolder != null) {
                data.putAll(dataHolder.getAllData());
            }
        }

        private Map<String, Object> getAllData() {
            return data;
        }


        protected String description() {
            return additionallySpecifiedFields.entrySet().stream()
                    .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
                    .collect(Collectors.joining(", "));
        }

        protected void clearDescriptionMeta() {
            additionallySpecifiedFields = new HashMap<>();
        }

    }
}
