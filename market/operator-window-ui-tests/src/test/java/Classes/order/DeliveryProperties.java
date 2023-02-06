package ui_tests.src.test.java.Classes.order;

public class DeliveryProperties {

    // Плановая дата доставки
    private String deliveryTimeFull;
    // Тип доставки
    private String typeDelivery;
    // Адрес доставки
    private String address;
    // Регион доставки
    private String region;
    // Получатель
    private String consignee;
    // Комментарий
    private String comment;
    // Трек-код
    private String trackCode;
    // Дата доставки при оформлении
    private String originalDeliveryTimeFull;
    // Расчетное время доставки
    private String estimatedDeliveryTime;
    // Наличие кнопки с данными курьера
    private Boolean isCourierDataButton;
    // Имя курьера
    private String firstNameCourier;
    // Телефон курьера
    private String phoneCourier;
    // Номер логистического заказа
    private String deliveryOrderNumber;
    // Ссылка на логистический заказ
    private String linkToDeliveryOrderPage;
    // Статус логистического заказа
    private String statusDeliveryOrder;
    // Количество коробок
    private String countOfBoxes;
    // Дата отгрузки
    private String dateOfShipment;
    // Вес заказа
    private String weightOfOrder;
    // Ссылка на мультизаказ
    private String linkToMultiOrderPage;
    // Текст из поля Также в составе мультизаказа
    private String numberOfMultiOrder;

    private boolean isDeliveryTimeFullFieldHighlighted;

    @Override
    public String toString() {
        return "DeliveryProperties{" +
                "deliveryTimeFull='" + deliveryTimeFull + '\'' +
                ", typeDelivery='" + typeDelivery + '\'' +
                ", address='" + address + '\'' +
                ", region='" + region + '\'' +
                ", consignee='" + consignee + '\'' +
                ", comment='" + comment + '\'' +
                ", trackCode='" + trackCode + '\'' +
                ", originalDeliveryTimeFull='" + originalDeliveryTimeFull + '\'' +
                ", estimatedDeliveryTime='" + estimatedDeliveryTime + '\'' +
                ", isCourierDataButton=" + isCourierDataButton +
                ", firstNameCourier='" + firstNameCourier + '\'' +
                ", phoneCourier='" + phoneCourier + '\'' +
                ", deliveryOrderNumber='" + deliveryOrderNumber + '\'' +
                ", linkToDeliveryOrderPage='" + linkToDeliveryOrderPage + '\'' +
                ", statusDeliveryOrder='" + statusDeliveryOrder + '\'' +
                ", countOfBoxes='" + countOfBoxes + '\'' +
                ", dateOfShipment='" + dateOfShipment + '\'' +
                ", weightOfOrder='" + weightOfOrder + '\'' +
                ", linkToMultiOrderPage='" + linkToMultiOrderPage + '\'' +
                ", numberOfMultiOrder='" + numberOfMultiOrder + '\'' +
                ", isDeliveryTimeFullFieldHighlighted=" + isDeliveryTimeFullFieldHighlighted +
                '}';
    }

    /**
     * Получить текст из поля Также в составе мультизаказа
     * @return
     */
    public String getNumberOfMultiOrder() {
        return numberOfMultiOrder;
    }

    /**
     * Задать текст в поле Также в составе мультизаказа
     * @return
     */
    public DeliveryProperties setNumberOfMultiOrder(String numberOfMultiOrder) {
        this.numberOfMultiOrder = numberOfMultiOrder;
        return this;
    }

    /**
     * Выделено ли поле Плановая дата доставки
     * @return
     */
    public boolean isDeliveryTimeFullFieldHighlighted() {
        return isDeliveryTimeFullFieldHighlighted;
    }

    /**
     * Задать статус выделения поля Плановая дата доставки
     * @param idDeliveryTimeFullFieldHighlighted выделено ли поле Плановая дата доставки
     * @return
     */
    public DeliveryProperties setDeliveryTimeFullFieldHighlighted(boolean idDeliveryTimeFullFieldHighlighted) {
        isDeliveryTimeFullFieldHighlighted = idDeliveryTimeFullFieldHighlighted;
        return this;
    }

    /**
     * Получить количество коробок
     *
     * @return
     */
    public String getCountOfBoxes() {
        return countOfBoxes;
    }

    /**
     * Указать количество коробок
     *
     * @param countOfBoxes количество коробок
     * @return
     */
    public DeliveryProperties setCountOfBoxes(String countOfBoxes) {
        this.countOfBoxes = countOfBoxes;
        return this;
    }

    /**
     * получить дату отгрузки
     *
     * @return
     */
    public String getDateOfShipment() {
        return dateOfShipment;
    }

    /**
     * указаьт дату отгрузки
     *
     * @param dateOfShipment дата отгрузки
     * @return
     */
    public DeliveryProperties setDateOfShipment(String dateOfShipment) {
        this.dateOfShipment = dateOfShipment;
        return this;
    }

    /**
     * Получить вес заказа
     *
     * @return
     */
    public String getWeightOfOrder() {
        return weightOfOrder;
    }

    /**
     * Указать вес заказ
     *
     * @param weightOfOrder вес заказа
     * @return
     */
    public DeliveryProperties setWeightOfOrder(String weightOfOrder) {
        this.weightOfOrder = weightOfOrder;
        return this;
    }

    /**
     * получить ссылку на логистический заказ
     *
     * @return
     */
    public String getLinkToDeliveryOrderPage() {
        return linkToDeliveryOrderPage;
    }

    /**
     * указать ссылку на логистический заказ
     *
     * @param linkToDeliveryOrderPage ссылка на логистический заказ
     * @return
     */
    public DeliveryProperties setLinkToDeliveryOrderPage(String linkToDeliveryOrderPage) {
        this.linkToDeliveryOrderPage = linkToDeliveryOrderPage;
        return this;
    }

    /**
     * получить ссылку на мультизаказ
     *
     * @return
     */
    public String getLinkToMultiOrderPage() {
        return linkToMultiOrderPage;
    }

    /**
     * указать ссылку на мультизаказ
     *
     * @param linkToMultiOrderPage ссылка на логистический заказ
     * @return
     */
    public DeliveryProperties setLinkToMultiOrderPage(String linkToMultiOrderPage) {
        this.linkToMultiOrderPage = linkToMultiOrderPage;
        return this;
    }

    /**
     * Получить имя курьера
     *
     * @return
     */
    public String getFirstNameCourier() {
        return firstNameCourier;
    }

    /**
     * Указать имя курьера
     *
     * @param firstNameCourier имя курьера
     * @return
     */
    public DeliveryProperties setFirstNameCourier(String firstNameCourier) {
        this.firstNameCourier = firstNameCourier;
        return this;
    }

    /**
     * Получить номер курьера
     *
     * @return
     */
    public String getPhoneCourier() {
        return phoneCourier;
    }

    /**
     * указать номер курьера
     *
     * @param phoneCourier номер курьера
     * @return
     */
    public DeliveryProperties setPhoneCourier(String phoneCourier) {
        this.phoneCourier = phoneCourier;
        return this;
    }

    /**
     * видна ли кнопка с курьером
     *
     * @return
     */
    public boolean isCourierDataButton() {
        return isCourierDataButton;
    }

    /**
     * Указать видимость кнопки с курьером
     *
     * @param courierDataButton
     * @return
     */
    public DeliveryProperties setCourierDataButton(boolean courierDataButton) {
        isCourierDataButton = courierDataButton;
        return this;
    }

    /**
     * Получить расчетное время доставки
     *
     * @return
     */
    public String getEstimatedDeliveryTime() {
        return estimatedDeliveryTime;
    }

    /**
     * указать расчетное время доставки
     *
     * @param estimatedDeliveryTime расчетное время доставки
     * @return
     */
    public DeliveryProperties setEstimatedDeliveryTime(String estimatedDeliveryTime) {
        this.estimatedDeliveryTime = estimatedDeliveryTime;
        return this;
    }

    /**
     * Получить дату доставки при оформлении
     *
     * @return
     */
    public String getOriginalDeliveryTimeFull() {
        return originalDeliveryTimeFull;
    }

    /**
     * указать дату доставки при оформлении
     *
     * @param originalDeliveryTimeFull дата доставки при оформлении
     * @return
     */
    public DeliveryProperties setOriginalDeliveryTimeFull(String originalDeliveryTimeFull) {
        this.originalDeliveryTimeFull = originalDeliveryTimeFull;
        return this;
    }

    /**
     * Получить трек-код
     *
     * @return
     */
    public String getTrackCode() {
        return trackCode;
    }

    /**
     * Указать трек-код
     *
     * @param trackCode трек-код
     * @return
     */
    public DeliveryProperties setTrackCode(String trackCode) {
        this.trackCode = trackCode;
        return this;
    }

    /**
     * Получить плановую дату лоставки
     *
     * @return
     */
    public String getDeliveryTimeFull() {
        return deliveryTimeFull;
    }

    /**
     * Указать плановую дату доставки
     *
     * @param deliveryTimeFull плановая дата доставки
     * @return
     */
    public DeliveryProperties setDeliveryTimeFull(String deliveryTimeFull) {
        this.deliveryTimeFull = deliveryTimeFull;
        return this;
    }

    /**
     * Получить тип доставки
     *
     * @return
     */
    public String getTypeDelivery() {
        return typeDelivery;
    }

    /**
     * Указать тип доставки
     *
     * @param typeDelivery тип доставки
     * @return
     */
    public DeliveryProperties setTypeDelivery(String typeDelivery) {
        this.typeDelivery = typeDelivery;
        return this;
    }

    /**
     * Получить адрес доставки
     *
     * @return
     */
    public String getAddress() {
        return address;
    }

    /**
     * Указать адрес доставки
     *
     * @param address адрес доставки
     * @return
     */
    public DeliveryProperties setAddress(String address) {
        this.address = address;
        return this;
    }

    /**
     * Указать регион доставки
     *
     * @param region регион доставки
     * @return
     */
    public DeliveryProperties setRegion(String region) {
        this.region = region;
        return this;
    }

    /**
     * Получить получателя
     *
     * @return
     */
    public String getConsignee() {
        return consignee;
    }

    /**
     * Указать получателя
     *
     * @param consignee получатель
     * @return
     */
    public DeliveryProperties setConsignee(String consignee) {
        this.consignee = consignee;
        return this;
    }

    /**
     * Получить комментарий
     *
     * @return
     */
    public String getComment() {
        return comment;
    }

    /**
     * Указать комментарий
     *
     * @param comment комментарий
     * @return
     */
    public DeliveryProperties setComment(String comment) {
        this.comment = comment;
        return this;
    }

    /**
     * Получить номер логистического заказа с карточки заказа беру
     *
     * @return
     */
    public String getDeliveryOrderNumber() {
        return deliveryOrderNumber;
    }

    /**
     * Указать номер логистического заказа с карточки заказа беру
     *
     * @param deliveryOrderNumber номер логистического заказ
     * @return
     */
    public DeliveryProperties setDeliveryOrderNumber(String deliveryOrderNumber) {
        this.deliveryOrderNumber = deliveryOrderNumber;
        return this;
    }

    /**
     * Получить статус логистического заказа
     *
     * @return
     */
    public String getStatusDeliveryOrder() {
        return statusDeliveryOrder;
    }

    /**
     * Указать статус логистического заказа
     *
     * @param statusDeliveryOrder статус логистического заказа
     * @return
     */
    public DeliveryProperties setStatusDeliveryOrder(String statusDeliveryOrder) {
        this.statusDeliveryOrder = statusDeliveryOrder;
        return this;
    }

    @Override
    public boolean equals(Object actual) {
        if (actual == this) {
            return true;
        }
        if (actual == null || actual.getClass() != this.getClass()) {
            return false;
        }

        DeliveryProperties actualDeliveryProperties = (DeliveryProperties) actual;

        if (this.deliveryTimeFull != null) {
            if (!deliveryTimeFull.equals(actualDeliveryProperties.deliveryTimeFull)) {
                return false;
            }
        }
        if (this.typeDelivery != null) {
            if (!typeDelivery.equals(actualDeliveryProperties.typeDelivery)) {
                return false;
            }
        }
        if (this.address != null) {
            if (!address.equals(actualDeliveryProperties.address)) {
                return false;
            }
        }
        if (this.region != null) {
            if (!region.equals(actualDeliveryProperties.region)) {
                return false;
            }
        }
        if (this.consignee != null) {
            if (!consignee.equals(actualDeliveryProperties.consignee)) {
                return false;
            }
        }
        if (this.comment != null) {
            if (!comment.equals(actualDeliveryProperties.comment)) {
                return false;
            }
        }
        if (this.trackCode != null) {
            if (!trackCode.equals(actualDeliveryProperties.trackCode)) {
                return false;
            }
        }

        if (this.originalDeliveryTimeFull != null) {
            if (!originalDeliveryTimeFull.equals(actualDeliveryProperties.originalDeliveryTimeFull)) {
                return false;
            }
        }

        if (this.estimatedDeliveryTime != null) {
            if (!estimatedDeliveryTime.equals(actualDeliveryProperties.estimatedDeliveryTime)) {
                return false;
            }
        }

        if (this.isCourierDataButton != null) {
            if (isCourierDataButton != actualDeliveryProperties.isCourierDataButton) {
                return false;
            }
        }

        if (this.firstNameCourier != null) {
            if (!firstNameCourier.equals(actualDeliveryProperties.firstNameCourier)) {
                return false;
            }
        }

        if (this.phoneCourier != null) {
            if (!phoneCourier.equals(actualDeliveryProperties.phoneCourier)) {
                return false;
            }
        }

        if (this.statusDeliveryOrder != null) {
            if (!statusDeliveryOrder.equals(actualDeliveryProperties.statusDeliveryOrder)) {
                return false;
            }
        }

        if (this.deliveryOrderNumber != null) {
            if (!deliveryOrderNumber.equals(actualDeliveryProperties.deliveryOrderNumber)) {
                return false;
            }
        }

        if (this.weightOfOrder != null) {
            if (!weightOfOrder.equals(actualDeliveryProperties.weightOfOrder)) {
                return false;
            }
        }

        if (this.countOfBoxes != null) {
            if (!countOfBoxes.equals(actualDeliveryProperties.countOfBoxes)) {
                return false;
            }
        }

        if (this.dateOfShipment != null) {
            if (!dateOfShipment.equals(actualDeliveryProperties.dateOfShipment)) {
                return false;
            }
        }

        if (this.estimatedDeliveryTime != null) {
            if (!estimatedDeliveryTime.equals(actualDeliveryProperties.estimatedDeliveryTime)) {
                return false;
            }
        }

        if (this.linkToDeliveryOrderPage != null) {
            if (!linkToDeliveryOrderPage.equals(actualDeliveryProperties.linkToDeliveryOrderPage)) {
                return false;
            }
        }
        return true;
    }
}
