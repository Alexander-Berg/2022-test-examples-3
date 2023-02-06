/**
 * TestingStartedEvent.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses;

public class TestingStartedEvent  extends org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.Event  implements java.io.Serializable {
    private java.lang.String ID;

    public TestingStartedEvent() {
    }

    public TestingStartedEvent(
           java.lang.Float alcoholLevel,
           org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.EBreathalyzerUnit alcoholUnit,
           java.lang.String detail,
           org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.Employee employee,
           org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.EventTypes eventsType,
           java.lang.Boolean isDeleted,
           java.util.Calendar localTime,
           java.lang.Boolean mask,
           java.lang.String oid,
           org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.Source source2,
           java.lang.Float temp,
           java.util.Calendar timeUTC,
           java.lang.String timeUTCLocalWithTimeZoneOffset,
           org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.TimeZone timeZone,
           org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.TimexOperator timexOperator,
           java.lang.String typeDescription,
           java.lang.String ID) {
        super(
            alcoholLevel,
            alcoholUnit,
            detail,
            employee,
            eventsType,
            isDeleted,
            localTime,
            mask,
            oid,
            source2,
            temp,
            timeUTC,
            timeUTCLocalWithTimeZoneOffset,
            timeZone,
            timexOperator,
            typeDescription);
        this.ID = ID;
    }


    /**
     * Gets the ID value for this TestingStartedEvent.
     * 
     * @return ID
     */
    public java.lang.String getID() {
        return ID;
    }


    /**
     * Sets the ID value for this TestingStartedEvent.
     * 
     * @param ID
     */
    public void setID(java.lang.String ID) {
        this.ID = ID;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof TestingStartedEvent)) return false;
        TestingStartedEvent other = (TestingStartedEvent) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.ID==null && other.getID()==null) || 
             (this.ID!=null &&
              this.ID.equals(other.getID())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = super.hashCode();
        if (getID() != null) {
            _hashCode += getID().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(TestingStartedEvent.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://schemas.datacontract.org/2004/07/ArmoSystems.Timex.SDKService.SDKClasses", "TestingStartedEvent"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("ID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://schemas.datacontract.org/2004/07/ArmoSystems.Timex.SDKService.SDKClasses", "ID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
