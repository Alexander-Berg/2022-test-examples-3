# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from travel.rasp.train_api.train_partners.ufs.base import UFS_ENCODING, UFS_OPERATION_STATUS_TO_OPERATION_STATUS
from travel.rasp.train_api.train_purchase.core.enums import OperationStatus


def add_ufs_encoding(xml_text):
    return '<?xml version="1.0" encoding="{}"?>\n{}'.format(UFS_ENCODING, xml_text)


def make_trans_info_response(order, override=None):
    override = override or {}
    passenger_overrides = override.get('passengers', [{}] * len(order.passengers))
    passengers = '\n'.join(make_passenger_xml_text(p, po) for p, po in zip(order.passengers, passenger_overrides))
    blank_params_by_blank_id = override.get('blanks', {})
    blanks = '\n'.join(
        make_blank_xml_text(t, blank_params_by_blank_id.get(t.blank_id))
        for t in order.iter_tickets()
    )

    ufs_status = {v: k for k, v in UFS_OPERATION_STATUS_TO_OPERATION_STATUS.items()}[
        override.get('status', OperationStatus.OK)]

    return add_ufs_encoding('''
    <UFS_RZhD_Gate>
      <System>
        <CurrentTime timeOffset="+03:00">25.09.2017 21:52:21</CurrentTime>
      </System>
      <TransID>106661900</TransID>
      <PrevTransID>0</PrevTransID>
      <Lang>RU</Lang>
      <LastRefundTransID>0</LastRefundTransID>
      <STAN/>
      <TStatus>{status}</TStatus>
      <RStatus>132</RStatus>
      <OrderNum>37313936809201</OrderNum>
      <SegmentType>1</SegmentType>
      <Type>1</Type>
      <CreateTime timeOffset="+03:00">25.09.2017 21:52:19</CreateTime>
      <BookingTime timeOffset="+03:00">25.09.2017 21:52:00</BookingTime>
      <ConfirmTime timeOffset="+03:00">25.09.2017 21:52:21</ConfirmTime>
      <ConfirmTimeLimit timeOffset="+03:00">25.09.2017 22:07:00</ConfirmTimeLimit>
      <Amount>2425.60</Amount>
      <Fee>0.00</Fee>
      <PlaceCount>2</PlaceCount>
      <UfsProfit>0.00</UfsProfit>
      <TrainNum>016А</TrainNum>
      <CarNum>07 </CarNum>
      <CarType>П</CarType>
      <DepartTime timeOffset="+03:00" timeType="0">02.11.2017 00:41:00</DepartTime>
      <DeltaDepartureLocalDate>0</DeltaDepartureLocalDate>
      <DeltaArrivalLocalDate>0</DeltaArrivalLocalDate>
      <Phone/>
      <Email/>
      <ServiceClass>3Д</ServiceClass>
      <StationFrom Code="2006004">МОСКВА ОКТЯБРЬСКАЯ</StationFrom>
      <StationTo Code="2004006">САНКТ-ПЕТЕРБУРГ ЛАДОЖ.</StationTo>
      <GenderClass>0</GenderClass>
      <ArrivalTime timeOffset="+03:00" timeType="0">02.11.2017 09:13:00</ArrivalTime>
      <Carrier>ФПК / ОАО "ФПК"</Carrier>
      <CarrierInn>7708709686</CarrierInn>
      <TimeDescription>КУРИТЬ ЗАПРЕЩЕНО. ВРЕМЯ ОТПР И ПРИБ МОСКОВСКОЕ</TimeDescription>
      <GroupDirection>0</GroupDirection>
      <Terminal>YAONLINE_TEST</Terminal>
      <IsTest>1</IsTest>
      <IsSuburbanTrain>0</IsSuburbanTrain>
      <ExpierSetEr timeOffset="+03:00">01.11.2017 23:41:00</ExpierSetEr>
      <ExpireSetEr timeOffset="+03:00">01.11.2017 23:41:00</ExpireSetEr>
      <Domain>yandex.ufs-online.ru</Domain>
      <PayTypeID>CC</PayTypeID>
      <IsInternational>0</IsInternational>
      {blanks}
      {passengers}
      <Order Id="26570572" RootTransId="106661900">
        <OrderItems>
          <OrderItem>
            <TransID>106661900</TransID>
            <PrevTransID>0</PrevTransID>
            <Lang>RU</Lang>
            <LastRefundTransID>0</LastRefundTransID>
            <STAN/>
            <TStatus>0</TStatus>
            <RStatus>132</RStatus>
            <OrderNum>37313936809201</OrderNum>
            <SegmentType>1</SegmentType>
            <Type>1</Type>
            <CreateTime timeOffset="+03:00">25.09.2017 21:52:19</CreateTime>
            <BookingTime timeOffset="+03:00">25.09.2017 21:52:00</BookingTime>
            <ConfirmTime timeOffset="+03:00">25.09.2017 21:52:21</ConfirmTime>
            <ConfirmTimeLimit timeOffset="+03:00">25.09.2017 22:07:00</ConfirmTimeLimit>
            <Amount>2425.60</Amount>
            <Fee>0.00</Fee>
            <PlaceCount>2</PlaceCount>
            <UfsProfit>0.00</UfsProfit>
            <TrainNum>016А</TrainNum>
            <CarNum>07 </CarNum>
            <CarType>П</CarType>
            <DepartTime timeOffset="+03:00" timeType="0">02.11.2017 00:41:00</DepartTime>
            <DeltaDepartureLocalDate>0</DeltaDepartureLocalDate>
            <DeltaArrivalLocalDate>0</DeltaArrivalLocalDate>
            <Phone/>
            <Email/>
            <ServiceClass>3Д</ServiceClass>
            <StationFrom Code="2006004">МОСКВА ОКТЯБРЬСКАЯ</StationFrom>
            <StationTo Code="2004006">САНКТ-ПЕТЕРБУРГ ЛАДОЖ.</StationTo>
            <GenderClass>0</GenderClass>
            <ArrivalTime timeOffset="+03:00" timeType="0">02.11.2017 09:13:00</ArrivalTime>
            <Carrier>ФПК / ОАО "ФПК"</Carrier>
            <CarrierInn>7708709686</CarrierInn>
            <TimeDescription>КУРИТЬ ЗАПРЕЩЕНО. ВРЕМЯ ОТПР И ПРИБ МОСКОВСКОЕ</TimeDescription>
            <GroupDirection>0</GroupDirection>
            <Terminal>YAONLINE_TEST</Terminal>
            <IsTest>1</IsTest>
            <IsSuburbanTrain>0</IsSuburbanTrain>
            <ExpierSetEr timeOffset="+03:00">01.11.2017 23:41:00</ExpierSetEr>
            <ExpireSetEr timeOffset="+03:00">01.11.2017 23:41:00</ExpireSetEr>
            <Domain>yandex.ufs-online.ru</Domain>
            <PayTypeID>CC</PayTypeID>
            <IsInternational>0</IsInternational>
            {blanks}
            {passengers}
          </OrderItem>
        </OrderItems>
        <Amount>2425.60</Amount>
        <ClientFee>0.00</ClientFee>
      </Order>
    </UFS_RZhD_Gate>
    '''.format(blanks=blanks, passengers=passengers, status=ufs_status))


def make_passenger_xml_text(passenger, override=None):
    override = override or {}
    return '''<Passenger ID="97281008" BlankID="{blank_id}">
      <Type>ПЛ</Type>
      <DocType>ПН</DocType>
      <DocNum>6406444444</DocNum>
      <Name>{last_name} {first_name} {patronymic}</Name>
      <Place>015</Place>
      <PlaceTier description="Нижнее">Н</PlaceTier>
      <R>МУЖ</R>
      <BirthDay>12.12.1990</BirthDay>
    </Passenger>'''.format(
        first_name=override.get('first_name', passenger.first_name.upper()),
        last_name=override.get('last_name', passenger.last_name.upper()),
        patronymic=override.get('patronymic', passenger.patronymic and passenger.patronymic.upper()),
        blank_id=override.get('blank_id', passenger.tickets[0].blank_id)
    )


def make_blank_xml_text(ticket, override=None):
    override = override or {}
    pending = override.get('pending', ticket.pending)
    return '''<Blank ID="{blank_id}" PrevID="0">
      <RetFlag>0</RetFlag>
      <Amount>1212.80</Amount>
      <AmountNDS>184.83</AmountNDS>
      <ServiceNDS>15.47</ServiceNDS>
      <ReservedSeatAmount>0.00</ReservedSeatAmount>
      <TicketAmount>0.00</TicketAmount>
      <TariffRateNds/>
      <ServiceRateNds/>
      <CommissionFeeRateNds/>
      <ReclamationCollectRateNds/>
      <TariffReturnNds/>
      <ServiceReturnNds/>
      <CommissionFeeReturnNds/>
      <ReclamationCollectReturnNds/>
      <TicketReturnAmount/>
      <ReservedSeatReturnAmount/>
      <ServiceReturnAmount/>
      <ReclamationCollectReturnAmount/>
      <TariffType>ПОЛНЫЙ</TariffType>
      <TicketNum>37313936809202</TicketNum>
      <RegTime timeOffset="+03:00">25.09.2017 21:52:21</RegTime>
      <RemoteCheckIn>{pending}</RemoteCheckIn>
      <PrintFlag>0</PrintFlag>
      <RzhdStatus>{rzhd_status}</RzhdStatus>
      <TicketToken/>
    </Blank>'''.format(
        blank_id=override.get('blank_id', ticket.blank_id),
        rzhd_status=override.get('rzhd_status', ticket.rzhd_status),
        pending='4' if pending else '1'
    )


def make_ufs_error_response(descr_id, code=32, descr='Ошибка'):
    return add_ufs_encoding("""
    <UFS_RZhD_Gate>
        <Error />
        <Code>{code}</Code>
        <DescrId>{descr_id}</DescrId>
        <Descr>{descr}</Descr>
    </UFS_RZhD_Gate>""".format(descr_id=descr_id, code=code, descr=descr))
