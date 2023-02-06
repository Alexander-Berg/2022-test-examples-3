package ru.yandex.market.delivery.deliveryintegrationtests.wms.client.socketserver;

// GENERATED FROM SQL:
// ===
// SELECT '/** '+COALESCE('Parameters: '+REPLACE(PARAMETERS,',',', '),'')
//      +' {@link '+THEDOMAIN+'.'+COMPOSITE+'}*/'+THEPROCNAME+','
// FROM wmsadmin.SPROCEDUREMAP
// ===
public enum WsRequestTypeEnum {
    /**
     * Parameters: userID, user_data_id {@link com.ssaglobal.scm.wms.service.dutilitymanagement.AddWSDefaultsP1S1}
     */
    ADDWSDEFAULTS,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.AdjustmentAPI}
     */
    AdjustmentAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.AdvancedShipNoticeAPI}
     */
    AdvancedShipNoticeAPI,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.ALSTD301}
     */
    ALSTD301,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.ALSTD302}
     */
    ALSTD302,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.ALSTD303}
     */
    ALSTD303,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.ALSTD306}
     */
    ALSTD306,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.AreaAPI}
     */
    AreaAPI,
    /**
     * Parameters: StorerKey, SKU, Ordertype, consolloc, maxlocqty, wavetotal, minwaveqty, pieceloc, caseloc,
     * include, loctype, wavekey, minlocqty, minreplenuom, forceuomreplenqty
     * {@link com.ssaglobal.scm.wms.service.dordermanagement.AssignConsolidatedLocP1S1}
     */
    ASSIGNCONSOLIDATEDLOC,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.AssociateAPI}
     */
    AssociateAPI,
    /**
     * Parameters: wavekey, newID {@link com.ssaglobal.scm.wms.service.dordermanagement.AutoSorting}
     */
    AUTOSORTFORBATCHPICKING,
    /**
     * Parameters: barcodeconfigkey, barcodeScanStr
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.BarcodeParser}
     */
    BARCODEPARSER,
    /**
     * Parameters: Wavekey, SortationStationKey
     * {@link com.ssaglobal.scm.wms.service.dordermanagement.BatchCandidatesP1S1}
     */
    BATCHCANDIDATES,
    /**
     * Parameters: Wavekey, SortationStationKey {@link com.ssaglobal.scm.wms.service.dordermanagement.BatchOrderP1S1}
     */
    BATCHORDER,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.BatchSelectionCriteriaAPI}
     */
    BatchSelectionCriteriaAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.BillToAPI}
     */
    BillToAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.BODListAPI}
     */
    BODListAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.CarrierAPI}
     */
    CarrierAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.CatchWeightAPI}
     */
    CatchWeightAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.CodeAPI}
     */
    CodeAPI,
    /**
     * Parameters: TASKDETAILKEY, Lot, Sku, ToLoc, FromId, ToID, FromLoc
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMPBPAConfirmP1S1}
     */
    ConfirmPaperPutaway,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.CustomerAPI}
     */
    CustomerAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.CycleCountAPI}
     */
    CycleCountAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.DemandAllocationAPI}
     */
    DemandAllocationAPI,
    /**
     * Parameters: orderkey {@link com.ssaglobal.scm.wms.service.dordermanagement.DockAssignmentP1S1}
     */
    DockAssignments,
    /**
     * Parameters: wavekey {@link com.ssaglobal.scm.wms.service.dordermanagement.DockAssingmentForWaveP1S1}
     */
    DockAssingmentForWaveP1S1,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.EquipmentAPI}
     */
    EquipmentAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.ErpLotAPI}
     */
    ErpLotAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.ExportInterfaceAPI}
     */
    ExportInterfaceAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.ExternStorerAPI}
     */
    ExternStorerAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.FacilityAPI}
     */
    FacilityAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.FacilityTransferAPI}
     */
    FacilityTransferAPI,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loc, checkType
     * {@link com.ssaglobal.scm.wms.service.fieldgroups.ValidLocation}
     */
    FGValidLocation,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storer, item,
     * checkType {@link com.ssaglobal.scm.wms.service.fieldgroups.ValidOwnerItem}
     */
    FGValidOwnerItem,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.FlowThruOrderAPI}
     */
    FlowThruOrderAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.FreightBillToAPI}
     */
    FreightBillToAPI,
    /**
     * {@link com.ssaglobal.scm.wms.service.dlabelprinting.GenerateLWSCertificate}
     */
    GenerateLWSCertificate,
    /**
     * Parameters: asnkey {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMPBPAP1S1}
     */
    GeneratePaperPutaway,
    /**
     * {@link com.ssaglobal.scm.wms.service.darchivemanagement.GetArchivingSetupP1S1}
     */
    GetArchiveDBSetup,
    /**
     * Parameters: wavekey, caller {@link com.ssaglobal.scm.wms.service.dordermanagement.GetConsolidatedSkuP1S1}
     */
    GETCONSOLIDATEDSKU,
    /**
     * Parameters: NestId {@link com.ssaglobal.scm.wms.service.dmultifacility.GetFacilities}
     */
    GETFACILITIES,
    /**
     * Parameters: InTransitKey, SourceNestId, TargetNestId, StorerKey, Notes
     * {@link com.ssaglobal.scm.wms.service.dmultifacility.IFTransfer}
     */
    IFTRANSFER,
    /**
     * Parameters: InTransitKey {@link com.ssaglobal.scm.wms.service.dmultifacility.IFTransferDelete}
     */
    IFTRANSFERDELETE,
    /**
     * Parameters: SourceNestId,  OrderKey {@link com.ssaglobal.scm.wms.service.dmultifacility.IFTransferDeleteSO}
     */
    IFTRANSFERDELETESO,
    /**
     * Parameters: InTransitKey,  LineNumber,  StorerKey,  SKU,  UOM,  PackKey,  Qty
     * {@link com.ssaglobal.scm.wms.service.dmultifacility.IFTransferDetail}
     */
    IFTRANSFERDETAIL,
    /**
     * Parameters: InTransitKey,  LineNumber {@link com.ssaglobal.scm.wms.service.dmultifacility.IFTransferDetailDelete}
     */
    IFTRANSFERDETAILDELETE,
    /**
     * Parameters: SourceNestId,  OrderKey,  LineNumber
     * {@link com.ssaglobal.scm.wms.service.dmultifacility.IFTransferDetailDeleteSO}
     */
    IFTRANSFERDETAILDELETESO,
    /**
     * Parameters: OrderKey,  LineNumber,  InTransitKey,  ReceiptKey,  StorerKey,  SKU,  UOM,  PackKey,  Qty
     * {@link com.ssaglobal.scm.wms.service.dmultifacility.IFTransferDetailSO}
     */
    IFTRANSFERDETAILSO,
    /**
     * Parameters: InTransitKey,  LineNumber,  UOM,  PackKey,  Qty
     * {@link com.ssaglobal.scm.wms.service.dmultifacility.IFTransferDetailUpdate}
     */
    IFTRANSFERDETAILUPDATE,
    /**
     * Parameters: OrderKey,  LineNumber,  InTransitKey,  ReceiptKey,  UOM,  PackKey,  OpenQty,  QtyShipped,  LOT,
     * ID {@link com.ssaglobal.scm.wms.service.dmultifacility.IFTransferDetailUpdateSO}
     */
    IFTRANSFERDETAILUPDATESO,
    /**
     * Parameters: TargetNestId, ReceiptKey, ReceiptLineNumber, QtyReceived, storerkey, sku
     * {@link com.ssaglobal.scm.wms.service.dmultifacility.IFTransferReceived}
     */
    IFTRANSFERRECEIVED,
    /**
     * Parameters: TargetNestId, ReceiptKey
     * {@link com.ssaglobal.scm.wms.service.dmultifacility.IFTransferReceivedForVerifyClose}
     */
    IFTRANSFERRECEIVEDFORVERIFYCLOSE,
    /**
     * Parameters: SourceNestId,  OrderKey,  OrderLineNumber,  QtyShipped
     * {@link com.ssaglobal.scm.wms.service.dmultifacility.IFTransferShipped}
     */
    IFTRANSFERSHIPPED,
    /**
     * Parameters: OrderKey, SourceNestId, TargetNestId, StorerKey, Notes
     * {@link com.ssaglobal.scm.wms.service.dmultifacility.IFTransferSO}
     */
    IFTRANSFERSO,
    /**
     * Parameters: InTransitKey, Notes {@link com.ssaglobal.scm.wms.service.dmultifacility.IFTransferUpdate}
     */
    IFTRANSFERUPDATE,
    /**
     * Parameters: SourceNestId,  OrderKey {@link com.ssaglobal.scm.wms.service.dmultifacility.IFTransferUpdateSO}
     */
    IFTRANSFERUPDATESO,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.InboundqcSamplingRulesAPI}
     */
    InboundqcSamplingRulesAPI,
    /**
     * Parameters: initialProcessHId, processHandleId, userid, sessionId, processName, tenantId, isBatch, nodeId,
     * componentId, facilityId, load, starttime
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.InstrumentInsertP1S1}
     */
    INSTRUMENTATIONINSERT,
    /**
     * Parameters: processHandleId {@link com.ssaglobal.scm.wms.service.dutilitymanagement.InstrumentUpdateP1S1}
     */
    INSTRUMENTATIONUPDATE,
    /**
     * Parameters: storerkey, groupcode, groupvalue, skufilter
     * {@link com.ssaglobal.scm.wms.service.dmultifacility.InvBalP1S1}
     */
    INVBAL,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.InventoryAPI}
     */
    InventoryAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.InventoryBalanceAPI}
     */
    InventoryBalanceAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.ItemIDAPI}
     */
    ItemIDAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.ItemMasterAPI}
     */
    ItemMasterAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.ITRNAPI}
     */
    ITRNAPI,
    /**
     * Parameters: LABELKEY {@link com.ssaglobal.scm.wms.service.dlabelprinting.LabelReprintP1S1}
     */
    LABELREPRINT,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.LoadAPI}
     */
    LoadAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.LocationAPI}
     */
    LocationAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.LotAPI}
     */
    LotAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.LotControlByShipToAPI}
     */
    LotControlByShipToAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.MasterBillOfLadingAPI}
     */
    MasterBillOfLadingAPI,
    /**
     * Parameters: CLIENTTABLE, INSTANCE, USERLIST
     * {@link com.ssaglobal.scm.wms.service.webservices.framework.MetaRFClientTableGC}
     */
    MetaRFClientTableGC,
    /**
     * Parameters: methodCalled, json {@link com.ssaglobal.scm.wms.service.webservices.MobileDynamicPickService}
     */
    MobileDynamicPickService,
    /**
     * Parameters: methodCalled, json {@link com.ssaglobal.scm.wms.service.webservices.MobileFormService}
     */
    MobileFormService,
    /**
     * Parameters: methodCalled, json {@link com.ssaglobal.scm.wms.service.webservices.MobileSerialNumberService}
     */
    MobileSerialService,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.MovableUnitAPI}
     */
    MovableUnitAPI,
    /**
     * {@link com.ssaglobal.scm.wms.service.orderallocation.NewAllocationStrategyP1S1}
     */
    NEWALLOCATESTRATEGY,
    /**
     * Parameters: storerkey, sku, loc {@link com.ssaglobal.scm.wms.service.dsupportmanagement.AddPickLocationP1S1}
     */
    NSP_ADDPICKLOCATION,
    /**
     * Parameters: workorderid, externwokey, sku, quantity
     * {@link com.ssaglobal.scm.wms.service.dworkcenter.AddWOVirtualComponents}
     */
    NSP_ADDWOVIRTUALCOMPONENTS,
    /**
     * Parameters: Storerkey,  Fromsku,  Fromloc,  ToLoc,  Fromid,  Toid,  Lot,  repkey,  packkey,  uom,  qty,
     * toqty, GROSSWGT1, NETWGT1, TAREWGT1 {@link com.ssaglobal.scm.wms.service.dwarehousemanagement.ConfirmRepP1S1}
     */
    NSP_CONFIRMREPL,
    /**
     * {@link com.ssaglobal.scm.wms.service.dwarehousemanagement.FillLotStorerKeySkuAP1S1}
     */
    NSP_FILL_LOT_STORERKEYSKU_A,
    /**
     * {@link com.ssaglobal.scm.wms.service.dwarehousemanagement.FillStorerkeySkuLotAP1S1}
     */
    NSP_FILL_STORERKEYSKU_LOT_A,
    /**
     * {@link com.ssaglobal.scm.wms.service.dwarehousemanagement.FillStorerkeySkuLotIdAP1S1}
     */
    NSP_FILL_STORERKEYSKULOT_ID_A,
    /**
     * Parameters: storerkey, sku,  lottable01,  lottable02,  lottable03,  lottable04,  lottable05, lottable06,
     * lottable07, lottable08, lottable09, lottable10
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.LotLookupP1S1}
     */
    NSP_LOTLOOKUP,
    /**
     * Parameters: workorderkey, externwokey, duedate, externwodate, quantity, parentorderkey, UDF1, UDF2, UDF3,
     * UDF4, UDF5, STORERKEY {@link com.ssaglobal.scm.wms.service.dworkcenter.ManualWOFactory}
     */
    NSP_MANUALWOFACTORY,
    /**
     * Parameters: workorderkey, externwokey, duedate, externwodate, quantity, parentorderkey, UDF1, UDF2, UDF3,
     * UDF4, UDF5, STORERKEY, ORDERKEY, ORDERLINENUMBER, WAVEKEY
     * {@link com.ssaglobal.scm.wms.service.dworkcenter.ManualWOFactoryWave}
     */
    NSP_MANUALWOFACTORY_WAVE,
    /**
     * Parameters: operationstep, workorderid, status, instructionsurl, notes, qtycomplete
     * {@link com.ssaglobal.scm.wms.service.dworkcenter.RouteOpsUpdate}
     */
    NSP_OPERATIONUPDATE,
    /**
     * Parameters: postmode {@link com.ssaglobal.scm.wms.service.dwarehousemanagement.PostPhysicalP1S1}
     */
    NSP_POST_PHYSICAL,
    /**
     * Parameters: receiptkey {@link com.ssaglobal.scm.wms.service.dutilitymanagement.ReceiptReversalP1S1}
     */
    NSP_RECEIPTREVERSAL,
    /**
     * Parameters: receiptkey, receiptlinenumbers
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.ReceiptReversalP1S1}
     */
    NSP_RECEIPTREVERSAL_CUSTOM,
    /**
     * Parameters: StartStorer, EndStorer, StartZone, EndZone, StartCom, EndCom, StartPick, EndPick, StartPriority,
     * EndPriority, RepNo, RepMethod {@link com.ssaglobal.scm.wms.service.dwarehousemanagement.ReplenishRepP1S1}
     */
    NSP_REPLREPORT,
    /**
     * Parameters: SkuByOpId, OperationId, WkcSkuId, QtyRequired, QtyUsed, Status
     * {@link com.ssaglobal.scm.wms.service.dworkcenter.UpdateOpComponentsP1S1}
     */
    NSP_UPDATEOPCOMPONENTS,
    /**
     * Parameters: workorderkey, externwokey, duedate, externwodate, quantity
     * {@link com.ssaglobal.scm.wms.service.dworkcenter.WOCreate}
     */
    NSP_WOCREATE,
    /**
     * Parameters: workorderid, status, quantity, qtycomplete, notes, instructionsurl
     * {@link com.ssaglobal.scm.wms.service.dworkcenter.WorkOrderUpdate}
     */
    NSP_WORKORDERUPDATE,
    /**
     * Parameters: loadid, stop, lpn {@link com.ssaglobal.scm.wms.service.dcrossdock.AddLPNToLoad}
     */
    NSPADDLPNTOLOAD,
    /**
     * Parameters: orderKeys {@link com.ssaglobal.scm.wms.service.dordermanagement.SendToTM}
     */
    NSPADDRFLOADCLOSEDCUSTOMERORDER,
    /**
     * Parameters: originalqty, targetqty, pickdetailkey, containerDetailId
     * {@link com.ssaglobal.scm.wms.service.dordermanagement.AdjustContainerQtyP1S1}
     */
    NSPAdjustContainerQtyP1S1,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.AL0107P1S1}
     */
    NSPAL01_07,
    /**
     * Parameters: orderkey, oskey, docarton, doroute, tblprefix, preallocateonly, atrace
     * {@link com.ssaglobal.scm.wms.service.dordermanagement.OrderProcessingP1S1}
     */
    NSPALLOCATIONTRACE,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.ALSTD01P1S1}
     */
    NSPALSTD01,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.ALSTD02P1S1}
     */
    NSPALSTD02,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.ALSTD03P1S1}
     */
    NSPALSTD03,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.ALSTD04P1S1}
     */
    NSPALSTD04,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.ALSTD04P1S1}
     */
    NSPALSTD05,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.ALSTD06P1S1}
     */
    NSPALSTD06,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.ALSTD07P1S1}
     */
    NSPALSTD07,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.ALSTD09P1S1}
     */
    NSPALSTD09,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.ALSTD10P1S1}
     */
    NSPALSTD10,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.ALSTD11P1S1}
     */
    NSPALSTD11,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.AL0202P1S1}
     */
    NSPALSTD202,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.AL0206P1S1}
     */
    NSPALSTD206,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.ALUC01P1S1}
     */
    NSPALUC01,
    /**
     * Parameters: asnkey, storerkey {@link com.ssaglobal.scm.wms.service.drfmanagement.ApplyLotMasking}
     */
    nspApplyLotMask,
    /**
     * Parameters: groupstart, groupend, useridstart, useridend, firstnamestart, firstnameend, lastnamestart,
     * lastnameend, statusstart, statusend, locale
     * {@link com.ssaglobal.scm.wms.service.dreportmanagement.AssociateStatusReportP1S1}
     */
    NSPASSOCIATESTATUS,
    /**
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.AutoRemoveHolds}
     */
    NSPAUTOREMOVEHOLDS,
    /**
     * Parameters: startloc, endloc, reclimit, locale
     * {@link com.ssaglobal.scm.wms.service.dreportmanagement.BatchReplenP1S1}
     */
    NSPBATCHREPLEN,
    /**
     * Parameters: BillingGroupMin, BillingGroupMax, StorerKeyMin, StorerKeyMax, ChargeTypes, CutOffDate, ActionKey
     * {@link com.ssaglobal.scm.wms.service.dbillingmanagement.BillInvoiceRunP1S1}
     */
    NSPBILLINGRUNWRAPPER,
    /**
     * Parameters: DropID {@link com.ssaglobal.scm.wms.service.drfmanagement.xBOLQueryP1S1}
     */
    NSPBOLQUERY,
    /**
     * Parameters: StorerKeyMin, StorerKeyMax, SkuMin, SkuMax, LotMin, LotMax, DateMin, DateMax, c_target,
     * c_target_object {@link com.ssaglobal.scm.wms.service.dreportmanagement.SPBSMWRPP1S1}
     */
    NSPBSMLDWRAPPER,
    /**
     * Parameters: wavekey, oskey, statusgroup {@link com.ssaglobal.scm.wms.service.dordermanagement.BuildWaveP1S1}
     */
    NSPBUILDWAVE,
    /**
     * Parameters: BatchID, EXBatchID, OrderSelectionKey
     * {@link com.ssaglobal.scm.wms.service.dordermanagement.BuildWPBatch}
     */
    NSPBuildWPBatch,
    /**
     * Parameters: WAVEKEY {@link com.ssaglobal.scm.wms.service.dordermanagement.CalcStdsEstimate}
     */
    NSPCALCSTDS,
    /**
     * Parameters: orderkey {@link com.ssaglobal.scm.wms.service.dordermanagement.CancelOrder}
     */
    NSPCANCELORDER,
    /**
     * Parameters: origin, sourcekey, Carrier, trailerkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.UpdateCarrierDetails}
     */
    nspCarrierUpdate,
    /**
     * Parameters: CartonBatch {@link com.ssaglobal.scm.wms.service.dordermanagement.CARTONIZATIONP1S1}
     */
    NSPCARTONIZATION,
    /**
     * Parameters: Loc {@link com.ssaglobal.scm.wms.service.dutilitymanagement.CheckDigitGeneration}
     */
    NSPCHECKDIGIT,
    /**
     * Parameters: dropid, childid, droploc
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.ServiceCheckDropIDP1S1}
     */
    NSPCHECKDROPID,
    /**
     * Parameters: userid, taskdetailkey, storerkey, sku, lot, FromLoc, FromID, ToLoc, ToId, qty
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMCHECKEQUIPPROFILEP1S1}
     */
    NSPCHECKEQUIPMENTPROFILE,
    /**
     * Parameters: storerkey, sku, lot, Loc, ID, qty
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMCheckMoveQtyP1S1}
     */
    NSPCHECKMOVEQTY,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, tag, insp_cfm,
     * rej_cfm {@link com.ssaglobal.scm.wms.service.drfmanagement.CheckQuarantineFlag}
     */
    NSPCHECKQUARANTINEFLAG,
    /**
     * Parameters: userid, taskdetailkey, tasktype, caseid, lot, FromLoc, FromID, ToLoc, ToId
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMCHECKSKIPTASKP1S1}
     */
    NSPCHECKSKIPTASKS,
    /**
     * {@link com.ssaglobal.scm.wms.service.dwarehousemanagement.ClearPhysicalP1S1}
     */
    NSPCLEARPHYSICAL,
    /**
     * Parameters: ORDERKEY, ORDERLINENUMBER, ALLOWBACKORDER, ORDERSTATUS, BACKORDERTYPE
     * {@link com.ssaglobal.scm.wms.service.dordermanagement.CloseOrders}
     */
    NSPCLOSEORDER,
    /**
     * Parameters: WAVEKEY {@link com.ssaglobal.scm.wms.service.dordermanagement.CloseOrderWaves}
     */
    NSPCLOSEORDERWAVE,
    /**
     * Parameters: groupstart, groupend, useridstart, useridend, firstnamestart, firstnameend, lastnamestart,
     * lastnameend, activitytypestart, activitytypeend, startdatestart, startdateend
     * {@link com.ssaglobal.scm.wms.service.dreportmanagement.CompletedActivityReportP1S1}
     */
    NSPCOMPLETEDACTIVITY,
    /**
     * Parameters: AssignmentNumber {@link com.ssaglobal.scm.wms.service.dtaskmanagement.UserActivityP1S1}
     */
    NSPCOMPLETEUSERACTIVITY,
    /**
     * Parameters: ORDERKEY, ORDERLINENUMBER, ROUTE, FLAG, ROW, BASEORDERKEY
     * {@link com.ssaglobal.scm.wms.service.dcrossdock.ConvertFlowThruOrder}
     */
    NSPCONVERTFLOWTHRUORDERS,
    /**
     * Parameters: userid, fromenterprise, fromfacilityname, tofacilityname
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.CopyConfiguration}
     */
    NSPCOPYCONFIG,
    /**
     * Parameters: storerKey, sku, fromloc, lot, fromid, toid, Toloc, qty, priority, user, releaseTime
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.CreateMoveTask}
     */
    NSPCreateMoveTask,
    /**
     * Parameters: orderlist, descr, wavekey {@link com.ssaglobal.scm.wms.service.dordermanagement.CreateWave}
     */
    NSPCREATEWAVE,
    /**
     * Parameters: tmpwavekey, descr, wavekey, appflag {@link com.ssaglobal.scm.wms.service.dordermanagement.CreateWave}
     */
    NSPCREATEWAVE1,
    /**
     * Parameters: filterid {@link com.ssaglobal.scm.wms.service.dordermanagement.CreateWaveByFilter}
     */
    NSPCREATEWAVEBYFILTER,
    /**
     * Parameters: cxadjustmentkey, storerkey, type
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.CXAdjustmentDetailReset}
     */
    NSPCXAdjustmentDetailReset,
    /**
     * Parameters: generatetask {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMCycleCountP1S1}
     */
    NSPCYCLECOUNT,
    /**
     * Parameters: lotxLocxIds,  generatetask
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.InventoryCycleCount.TMCycleCountInventoryP1S1}
     */
    NSPCYCLEINVCOUNT,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, user, msgsec
     * {@link com.ssaglobal.scm.wms.service.webservices.dao.mobileui.MetaRFMsgDAO}
     */
    nspdrfallmsg,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, msgsec
     * {@link com.ssaglobal.scm.wms.service.webservices.dao.mobileui.MetaRFMsgDAO}
     */
    nspdrfgetactiveusers,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.webservices.dao.mobileui.MetaRFMsgDAO}
     */
    nspdrfmsgpolling,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storer, item,
     * uom, image {@link com.ssaglobal.scm.wms.service.webservices.dao.mobileui.MetaRFImage}
     */
    nspdrfsaveimage01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, newconv, sender,
     * convid, msg, group, msgsec {@link com.ssaglobal.scm.wms.service.webservices.dao.mobileui.MetaRFMsgDAO}
     */
    nspdrfsendmsg,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * uom {@link com.ssaglobal.scm.wms.service.webservices.dao.mobileui.MetaRFMsgDAO}
     */
    nspdrfskuimage,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, user, msgsec
     * {@link com.ssaglobal.scm.wms.service.webservices.dao.mobileui.MetaRFMsgDAO}
     */
    nspdrfunsentmsg,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, user, type, data,
     * msgsec {@link com.ssaglobal.scm.wms.service.webservices.dao.mobileui.MetaRFMsgDAO}
     */
    nspdrfupdmsg,
    /**
     * Parameters: receiptkey, receiptlinenumber, labelname, printername, copies, isLabelToPrinted, explodeRecords
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.ExplodeASNLine}
     */
    NSPEXPLODEASNLINE,
    /**
     * Parameters: interactionid {@link com.ssaglobal.scm.wms.service.dutilitymanagement.FindInventoryDiscrepanciesP1S1}
     */
    NSPFINDINVDISCREP,
    /**
     * Parameters: OrderKey, OrderLineNumber, ProcessQty
     * {@link com.ssaglobal.scm.wms.service.dcrossdock.XFlowAllocationP1S1}
     */
    NSPFLOWTHROUGHALLOCATE,
    /**
     * Parameters: keyname, fieldlength, increment {@link com.ssaglobal.scm.wms.service.dutilitymanagement.GetKeyP1S1}
     */
    NSPG_GETKEY,
    /**
     * Parameters: storerkey, sku, lottable01, lottable02, lottable03, lottable04, lottable05, lottable06,
     * lottable07, lottable08, lottable09, lottable10
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.LotGenerationP1S1}
     */
    NSPG_LOTGEN,
    /**
     * Parameters: sourcekey,  masterbol,  isreportrequest,  forcegenerate
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.GenerateBOLNumberP1S1}
     */
    NSPGENBOLNUM,
    /**
     * Parameters: maskvalue {@link com.ssaglobal.scm.wms.service.dsupportmanagement.ValidateLottableMaskP1S1}
     */
    NSPGENERATEMASKVALIDATION,
    /**
     * Parameters: labelname {@link com.ssaglobal.scm.wms.service.dlabelprinting.GetLabelP1S1}
     */
    NSPGetLabel,
    /**
     * Parameters: labelnames {@link com.ssaglobal.scm.wms.service.dlabelprinting.GetLabelListP1S1}
     */
    NSPGetLabelList,
    /**
     * Parameters: StorerKey, Sku, Lottable01, Lottable02, Lottable03, Lottable04, Lottable05, Lottable06,
     * Lottable07, Lottable08, Lottable09, Lottable10
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.GetLotp1s1}
     */
    NSPGETLOT,
    /**
     * Parameters: STORERKEY, SKU, locale {@link com.ssaglobal.scm.wms.service.dcrossdock.ManualOppAllocP1S1}
     */
    NSPGETOPPORDERS,
    /**
     * Parameters: wavekey {@link com.ssaglobal.scm.wms.service.dordermanagement.GetOrderSummary}
     */
    NSPGETORDERSUMMARY,
    /**
     * Parameters: storerkey, sku, lot, loc, id
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.SERVICEGETPACKP1S1}
     */
    NSPGETPACK,
    /**
     * Parameters: inventoryholdkey, hold, status, holdloc, holdid, holdlot
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.HoldP1S1}
     */
    NSPHOLD,
    /**
     * Parameters: activity, userid, userattendancekey, starttime, duration, actype
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.IndirectActivityP1S1}
     */
    NSPINDIRECTACTIVITY,
    /**
     * Parameters: scriptId {@link com.ssaglobal.scm.wms.service.schedulearchive.IntiateScheduleArchive}
     */
    NSPIntiateScheduleArchive,
    /**
     * Parameters: InventoryHoldKey, holdlot, holdloc, holdid, status, hold, hldcmt
     * {@link com.ssaglobal.scm.wms.service.dwarehousemanagement.HoldResultSetP1S1}
     */
    NSPINVENTORYHOLDRESULTSET,
    /**
     * Parameters: ItrnSysId, StorerKey, Sku, Lot, ToLoc, ToID, Status, lottable01, lottable02, lottable03,
     * lottable04, lottable05, lottable06, lottable07, lottable08, lottable09, lottable10, casecnt, innerpack, Qty,
     * pallet, cube, grosswgt, netwgt, otherunit1, otherunit2, SourceKey, SourceType, PackKey, UOM, UOMCalc,
     * EffectiveDate {@link com.ssaglobal.scm.wms.service.dwarehousemanagement.AdjustmentP1S1}
     */
    NSPITRNADDADJUSTMENT,
    /**
     * Parameters: ItrnSysId, StorerKey, Sku, Lot, ToLoc, ToID, Status, lottable01, lottable02, lottable03,
     * lottable04, lottable05, lottable06, lottable07, lottable08, lottable09, lottable10, casecnt, innerpack, Qty,
     * pallet, cube, grosswgt, netwgt, otherunit1, otherunit2, SourceKey, SourceType, PackKey, UOM, UOMCalc,
     * EffectiveDate {@link com.ssaglobal.scm.wms.service.dutilitymanagement.RFAddDepositP1S1}
     */
    NSPITRNADDDEPOSIT,
    /**
     * Parameters: ItrnSysId, StorerKey, Sku, Lot, FromID, FromLoc, ToLoc, ToID, Status, lottable01, lottable02,
     * lottable03, lottable04, lottable05, lottable06, lottable07, lottable08, lottable09, lottable10, casecnt,
     * innerpack, Qty, pallet, cube, grosswgt, netwgt, otherunit1, otherunit2, SourceKey, SourceType, PackKey, UOM,
     * UOMCalc, EffectiveDate, Dummy1, Dummy2, Dummy3, TOGROSSWT, TONETWT, TOTAREWT
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.MoveP1S1}
     */
    NSPITRNADDMOVE,
    /**
     * Parameters: userid, assignmentnumber
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.LaborCompleteAssignmentP1S1}
     */
    NSPLABORCOMPLETEASSIGNMENT,
    /**
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.LaborMonitorCheckP1S1}
     */
    NSPLABORMONITORCHECK,
    /**
     * Parameters: userid, logout {@link com.ssaglobal.scm.wms.service.dtaskmanagement.LaborUserLoginP1S1}
     */
    NSPLABORUSERLOGIN,
    /**
     * Parameters: userid {@link com.ssaglobal.scm.wms.service.dtaskmanagement.LaborUserLogoutP1S1}
     */
    NSPLABORUSERLOGOUT,
    /**
     * Parameters: FromUI, LottableToProcess, Lottable01, Lottable02, Lottable03, Lottable04, Lottable05, Lottable06,
     * Lottable07, Lottable08, Lottable09, Lottable10, Storerkey, Sku
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.LC001P1S1}
     */
    NSPLC001,
    /**
     * Parameters: FromUI, LottableToProcess, Lottable01, Lottable02, Lottable03, Lottable04, Lottable05, Lottable06,
     * Lottable07, Lottable08, Lottable09, Lottable10, Storerkey, Sku
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.LC002P1S1}
     */
    NSPLC002,
    /**
     * Parameters: FromUI, LottableToProcess, Lottable01, Lottable02, Lottable03, Lottable04, Lottable05, Lottable06,
     * Lottable07, Lottable08, Lottable09, Lottable10, Storerkey, Sku
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.LC003P1S1}
     */
    NSPLC003,
    /**
     * Parameters: FromUI, LottableToProcess, Lottable01, Lottable02, Lottable03, Lottable04, Lottable05, Lottable06,
     * Lottable07, Lottable08, Lottable09, Lottable10, Storerkey, Sku
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.LC004P1S1}
     */
    NSPLC004,
    /**
     * Parameters: FromUI, LottableToProcess, Lottable01, Lottable02, Lottable03, Lottable04, Lottable05, Lottable06,
     * Lottable07, Lottable08, Lottable09, Lottable10, Storerkey, Sku
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.LC005P1S1}
     */
    NSPLC005,
    /**
     * Parameters: FromUI, LottableToProcess, Lottable01, Lottable02, Lottable03, Lottable04, Lottable05, Lottable06,
     * Lottable07, Lottable08, Lottable09, Lottable10, Storerkey, Sku
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.LC006P1S1}
     */
    NSPLC006,
    /**
     * Parameters: ReportID, RouteMin, RouteMax, DoorMin, DoorMax, LoadIDMin, LoadIDMax, DateMin, DateMax, offsetMin,
     * locale {@link com.ssaglobal.scm.wms.service.dreportmanagement.LoadMaintainP1S1}
     */
    NSPLOADMAINTENANCE,
    /**
     * Parameters: locstart, locend, loctypestart, loctypeend
     * {@link com.ssaglobal.scm.wms.service.dreportmanagement.LocationListP1S1}
     */
    NSPLOCATIONLIST,
    /**
     * Parameters: modulename, alertmessage, severity
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.LogAlertP1S1}
     */
    NSPLOGALERT,
    /**
     * Parameters: StorerKey, Sku, Lot, Loc, Id
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.ServiceLotLocIDUniqueRowP1S1}
     */
    NSPLOTLOCIDUNIQUEROW,
    /**
     * Parameters: loadplanningkey {@link com.ssaglobal.scm.wms.service.dcrossdock.RFLoadProcessing}
     */
    NSPLPCREATELOAD,
    /**
     * Parameters: loadplanningkey {@link com.ssaglobal.scm.wms.service.dcrossdock.RFApplyLaneData}
     */
    NSPLPLANE,
    /**
     * Parameters: loadplanningkey, scheduletype, deliverydate
     * {@link com.ssaglobal.scm.wms.service.dcrossdock.RFApplyLoadSchedule}
     */
    NSPLPLOADSCHED,
    /**
     * Parameters: LPN {@link com.ssaglobal.scm.wms.service.drfmanagement.LPNCheckDigitP1S1}
     */
    NSPLPNCHECKDIGIT01,
    /**
     * Parameters: FromUI, LottableToProcess, Lottable01, Lottable02, Lottable03, Lottable04, Lottable05, Lottable06,
     * Lottable07, Lottable08, Lottable09, Lottable10, Storerkey, Sku
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.LV001P1S1}
     */
    NSPLV001,
    /**
     * Parameters: FromUI, LottableToProcess, Lottable01, Lottable02, Lottable03, Lottable04, Lottable05, Lottable06,
     * Lottable07, Lottable08, Lottable09, Lottable10, Storerkey, Sku
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.LV002P1S1}
     */
    NSPLV002,
    /**
     * Parameters: FromUI, LottableToProcess, Lottable01, Lottable02, Lottable03, Lottable04, Lottable05, Lottable06,
     * Lottable07, Lottable08, Lottable09, Lottable10, Storerkey, Sku, receiptType
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.LV003P1S1}
     */
    NSPLV003,
    /**
     * Parameters: Orderkey {@link com.ssaglobal.scm.wms.service.dordermanagement.MassShip}
     */
    NSPMASSSHIPORDERS,
    /**
     * Parameters: Orderkey {@link com.ssaglobal.scm.wms.service.dordermanagement.MassShip2}
     */
    NSPMASSSHIPORDERS2,
    /**
     * Parameters: wavekey, action, route, door, stop, stage, carriercode, priority, mastrategy, nastrategy
     * {@link com.ssaglobal.scm.wms.service.dordermanagement.MassUpdateOrders}
     */
    NSPMASSUPDATEALL,
    /**
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.MonitorSlotting}
     */
    NSPMONITORSLOTTING,
    /**
     * Parameters: newcaseid, batchcontainerdetail {@link com.ssaglobal.scm.wms.service.dordermanagement.NewCaseIdP1S1}
     */
    NSPNewCaseIdP1S1,
    /**
     * Parameters: statusstart, statusend, wavekeystart, wavekeyend, ordernumberstart, ordernumberend, routestart,
     * routeend, stopstart, stopend, assignmentnumberstart, assignmentnumberend, groupstart, groupend, useridstart,
     * useridend, firstnamestart, firstnameend, lastnamestart, lastnameend
     * {@link com.ssaglobal.scm.wms.service.dreportmanagement.PlannedAssignmentsReportP1S1}
     */
    NSPOPENPLANNEDASSIGNMENTS,
    /**
     * Parameters: Storerkey, Sku, FromLocation, ToLocation, IsFromTemp, IsToTemp, IsAddfacing, IsDeleteFacing,
     * QtyLocLimit, OptBatchID, OptTaskSequence, NumberOfRecords
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.OptimizeMoveP1S1}
     */
    NSPOPTIMIZEMOVE,
    /**
     * Parameters: OrderKeyStart, OrderKeyEnd, StorerKeyStart, StorerKeyEnd, OrderDateStart, OrderDateEnd,
     * DeliveryDateStart, DeliveryDateEnd, TypeStart, TypeEnd, OrderGroupStart, OrderGroupEnd,
     * InterModalVehicleStart, InterModalVehicleEnd, ConsigneeKeyStart, ConsigneeKeyEnd, StatusStart, StatusEnd,
     * ExternOrderKeyStart, ExternOrderKeyEnd, PriorityStart, PriorityEnd
     * {@link com.ssaglobal.scm.wms.service.dreportmanagement.AktivP1S1}
     */
    NSPORDERPROCAKTIV,
    /**
     * Parameters: OrderKeyStart, OrderKeyEnd, StorerKeyStart, StorerKeyEnd, OrderDateStart, OrderDateEnd,
     * DeliveryDateStart, DeliveryDateEnd, TypeStart, TypeEnd, OrderGroupStart, OrderGroupEnd,
     * InterModalVehicleStart, InterModalVehicleEnd, ConsigneeKeyStart, ConsigneeKeyEnd, StatusStart, StatusEnd,
     * ExternOrderKeyStart, ExternOrderKeyEnd, PriorityStart, PriorityEnd
     * {@link com.ssaglobal.scm.wms.service.dreportmanagement.CaseGraphP1S1}
     */
    NSPORDERPROCCASEGRAPH,
    /**
     * Parameters: orderkey, oskey, docarton, doroute, tblprefix, preallocateonly
     * {@link com.ssaglobal.scm.wms.service.dordermanagement.OrderProcessingP1S1}
     */
    NSPORDERPROCESSING,
    /**
     * Parameters: OrderKeyStart, OrderKeyEnd, StorerKeyStart, StorerKeyEnd, OrderDateStart, OrderDateEnd,
     * DeliveryDateStart, DeliveryDateEnd, TypeStart, TypeEnd, OrderGroupStart, OrderGroupEnd,
     * InterModalVehicleStart, InterModalVehicleEnd, ConsigneeKeyStart, ConsigneeKeyEnd, StatusStart, StatusEnd,
     * ExternOrderKeyStart, ExternOrderKeyEnd, PriorityStart, PriorityEnd
     * {@link com.ssaglobal.scm.wms.service.dreportmanagement.CasePieP1S1}
     */
    NSPORDERPROCESSINGCASEPIE,
    /**
     * Parameters: OrderKeyStart, OrderKeyEnd, StorerKeyStart, StorerKeyEnd, OrderDateStart, OrderDateEnd,
     * DeliveryDateStart, DeliveryDateEnd, TypeStart, TypeEnd, OrderGroupStart, OrderGroupEnd,
     * InterModalVehicleStart, InterModalVehicleEnd, ConsigneeKeyStart, ConsigneeKeyEnd, StatusStart, StatusEnd,
     * ExternOrderKeyStart, ExternOrderKeyEnd, PriorityStart, PriorityEnd
     * {@link com.ssaglobal.scm.wms.service.dreportmanagement.PickGraphP1S1}
     */
    NSPORDERPROCPICKGRAPH,
    /**
     * Parameters: OrderKeyStart, OrderKeyEnd, StorerKeyStart, StorerKeyEnd, OrderDateStart, OrderDateEnd,
     * DeliveryDateStart, DeliveryDateEnd, TypeStart, TypeEnd, OrderGroupStart, OrderGroupEnd,
     * InterModalVehicleStart, InterModalVehicleEnd, ConsigneeKeyStart, ConsigneeKeyEnd, StatusStart, StatusEnd,
     * ExternOrderKeyStart, ExternOrderKeyEnd, PriorityStart, PriorityEnd
     * {@link com.ssaglobal.scm.wms.service.dreportmanagement.PickPieP1S1}
     */
    NSPORDERPROCPICKPIE,
    /**
     * Parameters: OrderKeyStart, OrderKeyEnd, StorerKeyStart, StorerKeyEnd, OrderDateStart, OrderDateEnd,
     * DeliveryDateStart, DeliveryDateEnd, TypeStart, TypeEnd, OrderGroupStart, OrderGroupEnd,
     * InterModalVehicleStart, InterModalVehicleEnd, ConsigneeKeyStart, ConsigneeKeyEnd, StatusStart, StatusEnd,
     * ExternOrderKeyStart, ExternOrderKeyEnd, PriorityStart, PriorityEnd
     * {@link com.ssaglobal.scm.wms.service.dreportmanagement.StatusGraphP1S1}
     */
    NSPORDERSTATUSGRAPH,
    /**
     * Parameters: orderkey {@link com.ssaglobal.scm.wms.service.dordermanagement.OutboundWorkflowP1S1}
     */
    NSPOUTBOUNDWORKFLOW,
    /**
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.PaperCycleCountP1S1}
     */
    NSPPAPERCYCLECOUNT,
    /**
     * Parameters: storerkey, sku, lot, Loc, ID, fromloc, fromid, qty, action
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMPendingMoveInUpdateP1S1}
     */
    NSPPENDINGMOVEINUPDATE,
    /**
     * Parameters: locstart, locend, zonestart, zoneend, areastart, areaend, StorerKeyMin, StorerKeyMax, SkuMin,
     * SkuMax, CountFilter {@link com.ssaglobal.scm.wms.service.dreportmanagement.PhysicalCountReportP1S1}
     */
    NSPPHYSICALCOUNTDETAIL,
    /**
     * Parameters: locstart, locend, zonestart, zoneend, areastart, areaend, StorerKeyMin, StorerKeyMax, SkuMin,
     * SkuMax, CountFilter, ComparisionType, locale
     * {@link com.ssaglobal.scm.wms.service.dreportmanagement.PhysicalDiscrepancyP1S1}
     */
    NSPPHYSICALDISCREPANCY,
    /**
     * Parameters: POKEY {@link com.ssaglobal.scm.wms.service.dutilitymanagement.POCloseP1S1}
     */
    NSPPOCLOSE,
    /**
     * Parameters: StorerKeyMin, StorerKeyMax, PoKeyMin, PoKeyMax, PoDateMin, PoDateMax
     * {@link com.ssaglobal.scm.wms.service.dreportmanagement.ComparePODiscP1S1}
     */
    NSPPODETAILDISCREP,
    /**
     * Parameters: inventoryqueryid
     * {@link com.ssaglobal.scm.wms.service.dcustomize.inventorycount.PopulateInventoryCountTasks}
     */
    NSPPOPINVTASK,
    /**
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.PopulateSlotData}
     */
    NSPPOPULATESLOTDATA,
    /**
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.PopulateUnSlotData}
     */
    NSPPOPULATEUNSLOTDATA,
    /**
     * Parameters: POKEY {@link com.ssaglobal.scm.wms.service.dutilitymanagement.POReOpenP1S1}
     */
    NSPPOREOPEN,
    /**
     * Parameters: QtyAvailable {@link com.ssaglobal.scm.wms.service.dutilitymanagement.PostAllocationValidationP1S1}
     */
    NSPPostAllocationValidation,
    /**
     * Parameters: controlKey, negadjreason, posadjreason
     * {@link com.ssaglobal.scm.wms.service.dwarehousemanagement.PostPhysicalControlP1S1}
     */
    NSPPOSTPHYSICALCONTROL,
    /**
     * Parameters: TaskDetailKey, Fromid {@link com.ssaglobal.scm.wms.service.dutilitymanagement.PostPickCheckIDP1S1}
     */
    NSPPOSTPICKCHECKID,
    /**
     * Parameters: TaskDetailKey, FromLoc, ToLoc
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.PostPickCheckLOCP1S1}
     */
    NSPPOSTPICKCHECKLOC,
    /**
     * Parameters: TaskDetailKey, Lot {@link com.ssaglobal.scm.wms.service.dutilitymanagement.PostPickCheckLOTP1S1}
     */
    NSPPOSTPICKCHECKLOT,
    /**
     * Parameters: TaskDetailKey, SKU {@link com.ssaglobal.scm.wms.service.dutilitymanagement.PostPickCheckSKUP1S1}
     */
    NSPPOSTPICKCHECKSKU,
    /**
     * Parameters: TaskDetailKey {@link com.ssaglobal.scm.wms.service.dutilitymanagement.PostPickNewLOTLOCIDP1S1}
     */
    NSPPOSTPICKNEWLOTLOCID,
    /**
     * Parameters: TaskDetailKey {@link com.ssaglobal.scm.wms.service.dutilitymanagement.PostPickShelfLifeCompareP1S1}
     */
    NSPPOSTPICKSHELFLIFECOMPARE,
    /**
     * Parameters: POKEY {@link com.ssaglobal.scm.wms.service.dutilitymanagement.POVerifiedCloseP1S1}
     */
    NSPPOVERIFIEDCLOSE,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.PR0107P1S1}
     */
    NSPPR01_07,
    /**
     * Parameters: orderkey, oskey, oprun, doroute
     * {@link com.ssaglobal.scm.wms.service.dordermanagement.PreallocationP1S1}
     */
    NSPPREALLOCATEORDERPROCESSING,
    /**
     * Parameters: wavekey {@link com.ssaglobal.scm.wms.service.dordermanagement.PreAllocateWave}
     */
    NSPPREALLOCATEWAVE,
    /**
     * Parameters: OrderKey, IText {@link com.ssaglobal.scm.wms.service.dutilitymanagement.PreShipShelfLifeCompareP1S1}
     */
    NSPPRESHIPSHELFLIFECOMPARE,
    /**
     * Parameters: OrderKey, IText
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.PreShipShipgroupValidationP1S1}
     */
    NSPPRESHIPSHIPGROUPVALIDATION,
    /**
     * Parameters: containerdetail, printername, rfidprintername
     * {@link com.ssaglobal.scm.wms.service.dlabelprinting.PrintContainerLabelP1S1}
     */
    NSPPrintContainerLabel,
    /**
     * Parameters: dropid, printer, copies
     * {@link com.ssaglobal.scm.wms.service.dlabelprinting.PrintAddLabelForDropIDP1S1}
     */
    NSPPRINTDROPIDLBL,
    /**
     * Parameters: labelname, printername, copies, parameters
     * {@link com.ssaglobal.scm.wms.service.dlabelprinting.PrintLabelP1S1}
     */
    NSPPrintLabel,
    /**
     * Parameters: labelname, printername, copies, parameters
     * {@link com.ssaglobal.scm.wms.service.dlabelprinting.PrintLabelsP1S1}
     */
    NSPPrintLabels,
    /**
     * Parameters: keytype, orderkey, orderline, printername, copies
     * {@link com.ssaglobal.scm.wms.service.dlabelprinting.PrintOrderLineLabelsP1S1}
     */
    NSPPrintOrderLineLabel,
    /**
     * Parameters: keytype, thekey, printername, rfidprintername, copies, assignment
     * {@link com.ssaglobal.scm.wms.service.dlabelprinting.PrintWaveLabelP1S1}
     */
    NSPPrintWaveLabel,
    /**
     * Parameters: rptId, waveKey {@link com.ssaglobal.scm.wms.service.dlabelprinting.PrintWaveReports}
     */
    NSPPRINTWAVEREPORT,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.PRSTD01P1S1}
     */
    NSPPRSTD01,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.PRSTD02P1S1}
     */
    NSPPRSTD02,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.PRSTD03P1S1}
     */
    NSPPRSTD03,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.PRSTD04P1S1}
     */
    NSPPRSTD04,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.PRSTD05P1S1}
     */
    NSPPRSTD05,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.PRSTD06P1S1}
     */
    NSPPRSTD06,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.PRSTD07P1S1}
     */
    NSPPRSTD07,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.PRSTD08P1S1}
     */
    NSPPRSTD08,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.PRSTD11P1S1}
     */
    NSPPRSTD11,
    /**
     * {@link com.ssaglobal.scm.wms.service.dstrategies.PRUC01P1S1}
     */
    NSPPRUC01,
    /**
     * {@link com.ssaglobal.scm.wms.service.dwarehousemanagement.PurgePhysicalP1S1}
     */
    NSPPURGEPHYSICAL,
    /**
     * {@link com.ssaglobal.scm.wms.service.dwarehousemanagement.PutInventoryonHoldforPhysicalP1S1}
     */
    NSPPUTINVENTORYONHOLDFORPHYSICAL,
    /**
     * Parameters: controlkey {@link com.ssaglobal.scm.wms.service.dwarehousemanagement.PutInventoryOnHoldNewPhysical}
     */
    NSPPUTINVENTORYONHOLDNEWPHYSICAL,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, fromsortstation,
     * orderkey, tosortstation {@link com.ssaglobal.scm.wms.service.drfmanagement.ReassignBatchOrder}
     */
    NSPREASSIGNBATCH,
    /**
     * Parameters: fromroute, fromloadid, fromstopid, Orderkey, toroute, toloadid, tostop
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReassignLoadUnitP1S1}
     */
    NSPREASSIGNLOADORDERDETAIL,
    /**
     * Parameters: fromroute, fromloadid, fromstopid, fromunitid, toroute, toloadid, tostop
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReassignLoadUnitP1S1}
     */
    NSPREASSIGNLOADUNIT,
    /**
     * Parameters: ReceiptKey {@link com.ssaglobal.scm.wms.service.dutilitymanagement.ReceiptCloseP1S1}
     */
    NSPRECEIPTCLOSE,
    /**
     * Parameters: StorerKeyMin, StorerKeyMax, ReceiptKeyMin, ReceiptKeyMax, ReceiptDateMin, ReceiptDateMax
     * {@link com.ssaglobal.scm.wms.service.dreportmanagement.CompareRDetDiscP1S1}
     */
    NSPRECEIPTDETAILDISCREP,
    /**
     * Parameters: ReceiptKey {@link com.ssaglobal.scm.wms.service.dutilitymanagement.ReceiptReOpenP1S1}
     */
    NSPRECEIPTREOPEN,
    /**
     * Parameters: ReceiptKey {@link com.ssaglobal.scm.wms.service.dutilitymanagement.ReceiptVerifiedCloseP1S1}
     */
    NSPRECEIPTVERIFIEDCLOSE,
    /**
     * Parameters: ReceiptKey, ReceiptType {@link com.ssaglobal.scm.wms.service.dutilitymanagement.ReceiveAll}
     */
    NSPRECEIVEALL,
    /**
     * Parameters: loadid, DoRelease {@link com.ssaglobal.scm.wms.service.dordermanagement.ReleaseLoadP1S1}
     */
    NSPRELEASELOAD,
    /**
     * {@link com.ssaglobal.scm.wms.service.dwarehousemanagement.ReleasePICountP1S1}
     */
    NSPRELEASEPICOUNT,
    /**
     * Parameters: wavekey, success, err, DoRelease
     * {@link com.ssaglobal.scm.wms.service.dordermanagement.ReleaseWaveP1S1}
     */
    NSPRELEASEWAVE,
    /**
     * Parameters: controlkey
     * {@link com.ssaglobal.scm.wms.service.dwarehousemanagement.RemoveInventoryOnHoldNewPhysical}
     */
    NSPREMINVENTORYONHOLDNEWPHYSICAL,
    /**
     * {@link com.ssaglobal.scm.wms.service.dwarehousemanagement.RemoveInventoryonHoldforPhysicalP1S1}
     */
    NSPREMOVEINVENTORYONHOLDFORPHYSICAL,
    /**
     * Parameters: zonestart, zoneend, locstart, locend, locale
     * {@link com.ssaglobal.scm.wms.service.dreportmanagement.ReplenishmentTopOffP1S1}
     */
    NSPREPLENISHMENTTOPOFF,
    /**
     * Parameters: keytype, thekey, printername, rfidprintername, copies, assignment
     * {@link com.ssaglobal.scm.wms.service.dlabelprinting.RePrintWaveLabelP1S1}
     */
    NSPRePrintWaveLabel,
    /**
     * Parameters: serialkey, interactionid
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.ResolveInventoryDiscrepanciesP1S1}
     */
    NSPRESOLVEINVDISCREP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sku
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.RFAboBarCodeParser}
     */
    NSPRFAboBarcode,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFListIndirectActivityP1S1}
     */
    NSPRFACTVLOOKUP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, AIPKName,
     * AIPKValue, theAIList {@link com.ssaglobal.scm.wms.service.drfmanagement.AIDefP1S1}
     */
    NSPRFAIDEF01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, route, stop,
     * date, continue, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14,
     * arg15, arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.AddIDToLoad}
     */
    NSPRFAIL10,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loadstopid,
     * caseid,  dropid {@link com.ssaglobal.scm.wms.service.drfmanagement.AddDropIdToLoad}
     */
    NSPRFAIL11,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storer, altsku
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFAILK01P1S1}
     */
    NSPRFAILK01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberAdjustmentP0S0}
     */
    NSPRFAJ00,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, id, loc,
     * storerkey, sku {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberAdjustmentP1S1}
     */
    NSPRFAJ01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * lot, id, loc, uom, packkey, cqty, tqty, aqty, reason, transactionkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberAdjustmentP2S2}
     */
    NSPRFAJ02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey,
     * altsku, sku, pro {@link com.ssaglobal.scm.wms.service.drfmanagement.AssociateAltSKU}
     */
    NSPRFALT01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * pro, other1, screen {@link com.ssaglobal.scm.wms.service.drfmanagement.GetSKUInfoP1S1}
     */
    NSPRFASNGSI01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * pro, lpn {@link com.ssaglobal.scm.wms.service.drfmanagement.GetSKUInfoP1S1}
     */
    NSPRFASNGSI02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * pro, other1, screen {@link com.ssaglobal.scm.wms.service.drfmanagement.RFGetMultipleAltSkuInfoP1S1}
     */
    NSPRFASNGSIMULTI,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * pro, pshka, orderkey {@link com.ssaglobal.scm.wms.service.dcustomize.RFRETGetMultipleAltSkuInfoP1S1}
     */
    NSPRFASNGSIMULTIRET,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * po, carrierref, vendoref, whref, externalasn, carriercode, containerref, rma, trailer, packslip, grn, id
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.ASNLookupP1S1}
     */
    NSPRFASNLKUP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, pro, storerkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.ASNSKULookupP1S1}
     */
    NSPRFASNSKULKUP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, area
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.AreaValidation}
     */
    NSPRFAV01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, SortStation,
     * Wavekey, CaseID, SKU, orderkey, lot1, lot2, lot3, lot4, lot5, lot6, lot7, lot8, lot9, lot10, lot11, lot12,
     * lotflag {@link com.ssaglobal.scm.wms.service.drfmanagement.ValidateIDandSKUP1S1}
     */
    NSPRFBAP10,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * pro {@link com.ssaglobal.scm.wms.service.dcustomize.bom.CreateRDForBOM}
     */
    NSPRFBOM0,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, bomCount, sku,
     * storerkey, descr, pro {@link com.ssaglobal.scm.wms.service.dcustomize.bom.ShowYesNoScreenBOM}
     */
    NSPRFBOM1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, bomSku, descrBom,
     * storerkey, masterSku, pro {@link com.ssaglobal.scm.wms.service.dcustomize.bom.CreateBOMatReceive}
     */
    NSPRFBOM2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, SortStation
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.WaveSortP1S1}
     */
    NSPRFBP01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, SortStation,
     * Wavekey, CaseID, SKU, orderkey {@link com.ssaglobal.scm.wms.service.drfmanagement.ValidateIDandSKUP1S1}
     */
    NSPRFBP04,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, SortStation,
     * Wavekey, CaseID, SKU, orderkey {@link com.ssaglobal.scm.wms.service.dcustomize.sorting.ValidateIDandSKUP1S1LT}
     */
    NSPRFBP04LT,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, SortStation,
     * Wavekey {@link com.ssaglobal.scm.wms.service.drfmanagement.UpdateStationWithWaveP1S1}
     */
    NSPRFBP06,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, qty, sortstation,
     * wavekey, caseid, sku, orderkey, sortlocation, confirmlocation, uom, confirmqty, notes, packkey,
     * transactionkey {@link com.ssaglobal.scm.wms.service.drfmanagement.GetContainerIDP1S1}
     */
    NSPRFBP07,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sortstation,
     * wavekey, caseid, sku, orderkey, confirmlocation, qty
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.CWProcessingP1S1}
     */
    NSPRFBP07A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, qty, sortstation,
     * wavekey, caseid, sku, orderkey, sortlocation, confirmlocation, uom, confirmqty, notes, packkey, serialnumber,
     * confirmLocChange {@link com.ssaglobal.scm.wms.service.dcustomize.sorting.GetContainerIDP1S1LT}
     */
    NSPRFBP07LT,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sortstation,
     * wavekey, caseid, olddropid, sortlocation, dropid, sku, confirmqty, cartontype, orderkey, confirmlocation, qty,
     * uom, packkey, transactionkey {@link com.ssaglobal.scm.wms.service.drfmanagement.CheckContainerIDP1S1}
     */
    NSPRFBP08,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, pickdetailkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.UpdatePickDetailOnESC}
     */
    NSPRFBP09,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sortstation,
     * wavekey, caseid, olddropid, sortlocation, dropid, pickdetailkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.GetCurrentIDP1S1}
     */
    NSPRFBP11,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, barcodeconfigkey,
     * barcodeScanStr {@link com.ssaglobal.scm.wms.service.dutilitymanagement.RFBarcodeParserP1S1}
     */
    NSPRFBRCD01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, identifier,
     * barcodescanstr, ASN, ORDER, OWNER, ITEM, PO
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.RFBarcodeParserP1S2}
     */
    NSPRFBRCD02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, cartongroup,
     * cartontype, type {@link com.ssaglobal.scm.wms.service.drfmanagement.CartonTypeLookupP1S1}
     */
    NSPRFCARTONLKUP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loc, lpn
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFCycleCountAddInventoryP1S1}
     */
    NSPRFCCADDINV,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loc, prev_rectype
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.CycleCountEmptyLocP1S1}
     */
    NSPRFCCEMPTYLOC,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, mode
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFCC0P1S1}
     */
    NSPRFCCLLI0,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, cckey, storerkey,
     * lot, sku, id, loc, qty, uom, packkey, taskkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFCCLLI01P1S1}
     */
    NSPRFCCLLI01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, cckey, storerkey,
     * lot, sku, id, loc, qty, uom, packkey, itrnkey, transactionkey, taskkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFCCLLI02P1S1}
     */
    NSPRFCCLLI02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, mode
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFCC0P1S1}
     */
    NSPRFCCSL0,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, cckey, storerkey,
     * sku, loc, qty, uom, packkey, taskkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFCCSL01P1S1}
     */
    NSPRFCCSL01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, cckey, taskkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFCCSL02P1S1}
     */
    NSPRFCCSL02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.webservices.dao.mobileui.MetaRFLoadConfig}
     */
    NSPRFCG01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, type, trankey,
     * sku {@link com.ssaglobal.scm.wms.service.drfmanagement.ChargelookupP1S1}
     */
    NSPRFCHARGELKUP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, type, trankey,
     * sku {@link com.ssaglobal.scm.wms.service.drfmanagement.ChargelookupP2S2}
     */
    NSPRFCHARGELKUP2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, fromid
     * {@link com.ssaglobal.scm.wms.service.dcustomize.CheckCartBeforeUnBuild}
     */
    NSPRFCHECKCART,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, StorerKey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.ChkAllowLPNGenerate}
     */
    NSPRFCHKALLOWLPNGENERATE,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, id, isConfirmed
     * {@link com.ssaglobal.scm.wms.service.dcustomize.CloseCartItrnInsert}
     */
    NSPRFCIINSERT,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sku, storerkey,
     * fromRF {@link com.ssaglobal.scm.wms.service.dcustomize.CheckIsLottableValidationConfigured}
     */
    NSPRFCILVC,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, fromid,
     * allowCloseCart {@link com.ssaglobal.scm.wms.service.dcustomize.CloseCartAction}
     */
    NSPRFCLOSECART,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderid,
     * externalorderid {@link com.ssaglobal.scm.wms.service.drfmanagement.RFORIDLOOKUPP1S1}
     */
    NSPRFCLSPRD1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, type
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.ContainerExchangeAdjGetKey}
     */
    NSPRFCOA,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.CodeLookupP1S1}
     */
    NSPRFCODELKUP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, type, key,
     * accountno {@link com.ssaglobal.scm.wms.service.dutilitymanagement.ContainerExchangeHelper}
     */
    NSPRFCOX1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, type, key,
     * partykey, palletype, qtyin, condin, qtyout, condout, externalkey, reasoncode
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.ContainerExchangeUpdate}
     */
    NSPRFCOX2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sku, storerkey
     * {@link com.ssaglobal.scm.wms.service.dcustomize.CheckPackAndCube}
     */
    NSPRFCPAC1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, eqmtid,
     * assignment  {@link com.ssaglobal.scm.wms.service.drfmanagement.AssignmentLookupP1S1}
     */
    NSPRFCRESLT,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, POSID
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFTransshipCloseQP1S1}
     */
    NSPRFCT01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, POSID, Comments,
     * transasnkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFTransshipClsCommentP1S1}
     */
    NSPRFCT02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, POSID, weight,
     * cube, transshipkey, transasnkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFTransshipWCP1S1}
     */
    NSPRFCT022,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, POSID, PALLETID,
     * SKU, EXPQty, RECQty, Over, Short, TransshipKey, transasnkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFTransshipCloseP1S1}
     */
    NSPRFCT03,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, order1, orderid,
     * keyfield, keytype, orderarg1, orderarg2, orderarg3, orderarg4, orderarg5, orderarg6, orderarg7, orderarg8,
     * orderarg9, orderarg10, orderarg11, orderarg12, orderarg13, orderarg14, orderarg15, orderarg16, orderarg17,
     * orderarg18, orderarg19, orderarg20, orderarg21, orderarg22, orderarg23, orderarg24, orderarg25, orderarg26,
     * orderarg27, orderarg28, orderarg29, orderarg30, casearg1, casearg2, casearg3, casearg4, casearg5, casearg6,
     * casearg7, casearg8, casearg9, casearg10, casearg11, casearg12, casearg13, casearg14, casearg15, casearg16,
     * casearg17, casearg18, casearg19, casearg20, casearg21, casearg22, casearg23, casearg24, casearg25, casearg26,
     * casearg27, casearg28, casearg29, casearg30
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFCartonSummaryP1S1}
     */
    NSPRFCTL01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, preprint, order1,
     * orderid, keyfield, keytype, orderarg1, orderarg2, orderarg3, orderarg4, orderarg5, orderarg6, orderarg7,
     * orderarg8, orderarg9, orderarg10, orderarg11, orderarg12, orderarg13, orderarg14, orderarg15, orderarg16,
     * orderarg17, orderarg18, orderarg19, orderarg20, orderarg21, orderarg22, orderarg23, orderarg24, orderarg25,
     * orderarg26, orderarg27, orderarg28, orderarg29, orderarg30, casearg1, casearg2, casearg3, casearg4, casearg5,
     * casearg6, casearg7, casearg8, casearg9, casearg10, casearg11, casearg12, casearg13, casearg14, casearg15,
     * casearg16, casearg17, casearg18, casearg19, casearg20, casearg21, casearg22, casearg23, casearg24,
     * casearg25, casearg26, casearg27, casearg28, casearg29, casearg30
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFCartonSummaryLabelPrintP1S1}
     */
    NSPRFCTLP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, areakey
     * {@link com.ssaglobal.scm.wms.service.dcustomize.dropcontrol.RFDropControlPermission}
     */
    NSPRFDC00P,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, dropidcaseid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDropIDQCorCloseP1S1}
     */
    NSPRFDC01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, dropidcaseid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDropIDQCPackOutP1S1}
     */
    NSPRFDC01A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, serialkey, id,
     * sku, lot, qtypicked, qtyinspected, qtyaccepted, qtyrejected, reason, udf1, udf2, udf3, outype, isclose
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDropIDQCPackOutCloseP1S1}
     */
    NSPRFDC02A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, id
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDropIDQCPackOutCloseUpdateP1S1}
     */
    NSPRFDC02B,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, dropid
     * {@link com.ssaglobal.scm.wms.service.dcustomize.dropcontrol.RFDropCloseP1S1}
     */
    NSPRFDCL01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, fromid
     * {@link com.ssaglobal.scm.wms.service.dcustomize;.CheckCartForDimensions}
     */
    NSPRFDIM1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, serialnumer,
     * repeatDimension, fromid {@link com.ssaglobal.scm.wms.service.dcustomize;.CheckSerialForDimensions}
     */
    NSPRFDIM3,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, length, width,
     * height, weight, toid, serialnumber {@link com.ssaglobal.scm.wms.service.dcustomize;.DoDimensions}
     */
    NSPRFDIM4,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, fromid,
     * allowConfirmDim {@link com.ssaglobal.scm.wms.service.dcustomize;.DImensionsEnd}
     */
    NSPRFDIM5,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toid,
     * serialnumber {@link com.ssaglobal.scm.wms.service.dcustomize;.MoveSerialToIdAtDimension}
     */
    NSPRFDIM6,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, fromid, lpn,
     * storerkey, sku, demandkey, qtytopick {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDynamicPickLotLkupP1S1}
     */
    NSPRFDPL01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, demandkey,
     * storerkey, sku, loc, lpn, idlabel, id, id2, id3, qtytopick, fromqty, qtypicked, lot
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDynamicPickLotP1S1}
     */
    NSPRFDPL2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, dropid, rectype,
     * areakey {@link com.ssaglobal.scm.wms.service.dcustomize.dropcontrol.RFDropControlP1S1}
     */
    NSPRFDRC01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, dropid, rectype,
     * child {@link com.ssaglobal.scm.wms.service.dcustomize.dropcontrol.RFDropControlP2S1}
     */
    NSPRFDRC02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, dropid
     * {@link com.ssaglobal.scm.wms.service.dcustomize.dropcontrol.RFDropControlP4S1}
     */
    NSPRFDRC04,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, dropid, calltype
     * {@link com.ssaglobal.scm.wms.service.dcustomize.dropcontrol.RFDropControlP5S1}
     */
    NSPRFDRC05,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, dropid, calltype
     * {@link com.ssaglobal.scm.wms.service.dcustomize.dropcontrol.RFDropControlP5S1}
     */
    NSPRFDRC06,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, param1, param2
     * {@link com.ssaglobal.scm.wms.service.webservices.dao.mobileui.MetaRFScreenDAO}
     */
    NSPRFDYNAADD01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, param1, param2
     * {@link com.ssaglobal.scm.wms.service.webservices.dao.mobileui.MetaRFScreenDAO}
     */
    NSPRFDYNACAT01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, param1, param2
     * {@link com.ssaglobal.scm.wms.service.webservices.dao.mobileui.MetaRFScreenDAO}
     */
    NSPRFDYNADIVIDE01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, param1, param2
     * {@link com.ssaglobal.scm.wms.service.webservices.dao.mobileui.MetaRFScreenDAO}
     */
    NSPRFDYNAMULTIPLY01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, param1, param2
     * {@link com.ssaglobal.scm.wms.service.webservices.dao.mobileui.MetaRFScreenDAO}
     */
    NSPRFDYNASUBTRACT01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, seq, question,
     * reply, format, anscode, startctr, endctr, eqmtid, eqmttype, stgykey, answhen, screen
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.EquipmentQuestions}
     */
    NSPRFEMQ01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, equipid,
     * assgnflag, owner {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLaborValidateEquipment}
     */
    NSPRFEQ1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, equipid,
     * assgnflag, owner {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLaborValidateEquipment}
     */
    NSPRFEQ2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, equipmentid,
     * startwork, owner {@link com.ssaglobal.scm.wms.service.drfmanagement.ValidateEquipment}
     */
    NSPRFEQID01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, equipmentid,
     * startwork, owner {@link com.ssaglobal.scm.wms.service.drfmanagement.ValidateEquipment}
     */
    NSPRFEQID01LB,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey,
     * externalorderid, receiptkey, currentretrec
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMPccCompleteP1S1}
     */
    NSPRFFPO01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, StorerKey, pokey,
     * receiptkey, sku, qty, uom, packkey, upc, printerid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.GenerateLPNP1S1}
     */
    NSPRFGENERATELPN,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, StorerKey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.GenerateLPNP1S1}
     */
    NSPRFGENERATELPN01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.GeneratePackID}
     */
    NSPRFGENERATEPACKID,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, StorerKey,
     * printerid {@link com.ssaglobal.scm.wms.service.drfmanagement.GeneratePalletIDP1S1}
     */
    NSPRFGENERATEPALLETID,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, width, length,
     * height, weight, dimhost {@link com.ssaglobal.scm.wms.service.dcustomize.GetDims}
     */
    NSPRFGETDIMS,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.GetUsersCurrentEquipment}
     */
    NSPRFGETEQ,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, fromloc
     * {@link com.ssaglobal.scm.wms.service.dcustomize.inventorycount.GetInventoryCountTasksP2S1}
     */
    NSPRFGETINVENTORYTASKS,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, areakey, sequence
     * {@link com.ssaglobal.scm.wms.service.dcustomize.inventorycount.GetInventoryCountTasksP1S1}
     */
    NSPRFGETINVTASK,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, caller,
     * storerkey, sku, taskdetailkey, caseid, orderkey, orderlinenumber
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFGetNotesP1S1}
     */
    NSPRFGN01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * screen {@link com.ssaglobal.scm.wms.service.drfmanagement.RFGetSkuInfoP1S1}
     */
    NSPRFGSI01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ID, sortstation
     * {@link com.ssaglobal.scm.wms.service.dcustomize.GetOrderWaveAtSort}
     */
    NSPRFGWO,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ID
     * {@link com.ssaglobal.scm.wms.service.dcustomize.sorting.GetOrderWaveAtSortLT}
     */
    NSPRFGWOLT,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, InventoryHoldKey,
     * holdlot, holdloc, holdid, status, hold, hldcmt
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.RFHoldP1S1}
     */
    NSPRFHCXM,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, item, loc, id,
     * lot {@link com.ssaglobal.scm.wms.service.dutilitymanagement.RetreiveInvHolds}
     */
    NSPRFHR1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, holdloc, holdid,
     * holdlot, user, date, status, hldcmt, InventoryHoldKey, hold, var1
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.RFRemoveHolds}
     */
    NSPRFHR3,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, id
     * {@link com.ssaglobal.scm.wms.service.dcustomize.IsIdEmpty}
     */
    NSPRFIIE,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, doctype, field0,
     * value0, field1, value1, field2, value2, field3, value3, field4, value4, field5, value5, field6, value6,
     * field7, value7, field8, value8, field9, value9, field10, value10, field11, value11, field12, value12, field13,
     * value13, field14, value14, image {@link com.ssaglobal.scm.wms.service.webservices.dao.image.SaveImage}
     */
    NSPRFIMG01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, doctype, field0,
     * value0, field1, value1, field2, value2, field3, value3, field4, value4, field5, value5, field6, value6,
     * field7, value7, field8, value8, field9, value9, field10, value10, field11, value11, field12, value12, field13,
     * value13, field14, value14, image {@link com.ssaglobal.scm.wms.service.webservices.dao.image.QueryImage}
     */
    NSPRFIMG02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, assignnumber,
     * inventoryid, areakey, sequence
     * {@link com.ssaglobal.scm.wms.service.dcustomize.inventorycount.InventoryCountAssignmentConfirmP1S1}
     */
    NSPRFINVASSCONF,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, allowConfirm,
     * taskdetailkey, loc, assignmentnumber, inventoryId {@link com.ssaglobal.scm.wms.service.dcustomize.InvConfirm}
     */
    NSPRFINVCFRM,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, fromloc,
     * locconfirm, taskdetailkey, inventoryid, assignnumber, status, open
     * {@link com.ssaglobal.scm.wms.service.dcustomize.inventorycount.InventoryCountLocationConfirmP1S1}
     */
    NSPRFINVLOCCONF,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, fromloc,
     * fromlocconf, inventoryid, assignnumber
     * {@link com.ssaglobal.scm.wms.service.dcustomize.inventorycount.CreateNewInventoryTaskP1S1}
     */
    NSPRFINVNEWTASK,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, fromloc,
     * fromlocconf, inventoryid, assignnumber, curtaskdetailkey
     * {@link com.ssaglobal.scm.wms.service.dcustomize.inventorycount.CreateNewInventoryTaskP1S1}
     */
    NSPRFINVNEWTASK2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, serialnumber,
     * fromloc, locconfirm, taskdetailkey, inventoryid, assignnumber
     * {@link com.ssaglobal.scm.wms.service.dcustomize.inventorycount.InventoryCountSNConfP1S1}
     */
    NSPRFINVSNCONF,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * sku, id, loc, caseid, qty, uom, packkey, status, lottable01, lottable02, lottable03, lottable04, lottable05,
     * lottable06, lottable07, lottable08, lottable09, lottable10
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFInquiryP1S1}
     */
    NSPRFIQ01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, id
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFCIDLOOKUPP1S1}
     */
    NSPRFIQ04,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, dropid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDROPIDLOOKUPP1S1}
     */
    NSPRFIQ08,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderid,
     * externalorderid {@link com.ssaglobal.scm.wms.service.drfmanagement.RFORIDLOOKUPP1S1}
     */
    NSPRFIQ10,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loadid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLDIDLOOKUPP1S1}
     */
    NSPRFIQ11,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey,
     * currentretrec {@link com.ssaglobal.scm.wms.service.drfmanagement.RFOverPickCloseOrderP1S1}
     */
    NSPRFIQ140,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey,
     * currentretrec {@link com.ssaglobal.scm.wms.service.drfmanagement.RFOverPickCloseOrderP1S1}
     */
    NSPRFIQ140A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, id
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFCSSKULOOKUPP1S1}
     */
    NSPRFIQCSSKU,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sn, toid, fromid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFGetLotXLocXIdBySNP1S}
     */
    NSPRFIQSN01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sn, toid, fromid,
     * fromloc, lot {@link com.ssaglobal.scm.wms.service.dcustomize.RFGetLotXLocXIdBySNBOMCheck}
     */
    NSPRFIQSN01BOMD,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * sku, fromloc, fromid, toloc, toid, qty, uom, packkey, refnum, var1, var2, var3, SERIALNUMBER
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFMoveToIntransitP1S1}
     */
    NSPRFIQSN02X,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * lot, loc, trantype, serialnumber, lpn, sourcekey, sourcelinenumber
     * {@link com.ssaglobal.scm.wms.service.dcustomize.InsertSerialTemp}
     */
    NSPRFISN01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey,
     * externorderkey {@link com.ssaglobal.scm.wms.service.drfmanagement.ItemForOrderLookupP1S1}
     */
    NSPRFITEMLKUP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, assignment
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLaborAssignmentPickP1S1}
     */
    NSPRFLA01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, assignment
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLaborCompleteAssignmentP1S1}
     */
    NSPRFLA02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, assignment,
     * eqmtid, rectype {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLaborAssignmentPickP1S1}
     */
    NSPRFLAPK01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFGetEquipmentP1S1}
     */
    NSPRFLAPKGE,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, Id,
     * PrintChildLabels {@link com.ssaglobal.scm.wms.service.drfmanagement.PrintLabelP1S1}
     */
    NSPRFLBLPRNT01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, calltype,
     * taskkey, tasktype, subtask, reason, assignmentkey, clusterkey, taskkey2, taskkey3, taskkey4, taskkey5,
     * sequence {@link com.ssaglobal.scm.wms.service.dlabormanagement.LaborP1S1}
     */
    NSPRFLBRBEGIN,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, calltype,
     * taskkey, tasktype, subtask, reason, assignmentkey, clusterkey, taskkey2, taskkey3, taskkey4, taskkey5,
     * sequence {@link com.ssaglobal.scm.wms.service.dlabormanagement.LaborP1S1}
     */
    NSPRFLBREND,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loadid,
     * trailerid, door {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLoadCloseP1S1}
     */
    nspRFLC01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loadid, sealno,
     * carrier {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLoadCloseP3S3}
     */
    nspRFLC02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loadid, sealno,
     * carrier {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLoadCloseP2S2}
     */
    nspRFLCM01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, area, id, route,
     * door {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDirectedLoadQP1S1}
     */
    NSPRFLD10,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ID1, ID2,
     * sequence, position, FromLoc, route, Stop, TrailerID, door1, door2, chkdigit, TaskDetailID, area, temperature
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDirectedLoadUP1S1}
     */
    NSPRFLD11,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, rftaskid, area,
     * door, route, stop {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDirectedLoadStopSP1S1}
     */
    NSPRFLD12,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, rftaskid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDirectedLoadEscP1S1}
     */
    NSPRFLD13,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, rftaskid, area,
     * door, route, stop {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDirectedLoadAUP1S1}
     */
    NSPRFLD14,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, route, date
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLDINQUIRY01P1S1}
     */
    NSPRFLDIQ01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, route, stop, date
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLDINQUIRY02P1S1}
     */
    NSPRFLDIQ02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, route, stop, date
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLDINQUIRY03P1S1}
     */
    NSPRFLDIQ03,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLoadLPN0P1S1}
     */
    nspRFLDL0,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, lpn
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLoadLPNP1S1}
     */
    nspRFLDL01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, lpn, fromloc,
     * route, fromdoor, todoor, stop, orderkey, loadid, loadcube, loadwgt, loadqty, loadids, skucube, skuwgt, skuqty,
     * taskkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLoadLPNP2S2}
     */
    nspRFLDL02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLoadLPNEscP1S1}
     */
    NSPRFLDL3,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, lpn, fromloc,
     * route, fromdoor, todoor, stop, orderkey, loadid, taskkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLoadLPNP3S3}
     */
    nspRFLDLD02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, externloadid,
     * door, route, carrier {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLoadLookup}
     */
    NSPRFLDLKUP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, externloadid,
     * door, route, carrier {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLoadLookup}
     */
    NSPRFLDLKUP2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.CurrentTimeP1S1}
     */
    NSPRFLIA0,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, activity,
     * starttime, owner {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLaborIndirectActivityP1S1}
     */
    NSPRFLIA01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLaborEndActivityP1S1}
     */
    NSPRFLIA02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, logout
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLaborLoginP1S1}
     */
    NSPRFLL1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ConfirmLogout
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLaborLogoutP1S1}
     */
    NSPRFLLO1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLaborCheckP1S1}
     */
    NSPRFLM0,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLaborEndAssignment}
     */
    NSPRFLMA01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLaborEndAssignment}
     */
    NSPRFLMA02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLaborChangeOwnerP1S1}
     */
    NSPRFLMA04,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, owner
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLaborUpdateOwnerOnAssignmentP1S1}
     */
    NSPRFLMA04A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLaborCheckAssignment}
     */
    NSPRFLMA1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLaborPerfMonP1S1}
     */
    NSPRFLMP0,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, FieldName,
     * Description, TableName, Filter1, Filter2, Filter3, Filter4, Filter5, Filter6, Filter7, Filter8, Filter9,
     * Filter10, Filter11, Filter12, Filter13, Filter14, Filter15, Value1, Value2, Value3, Value4, Value5, Value6,
     * Value7, Value8, Value9, Value10, Value11, Value12, Value13, Value14, Value15
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.RFLookUpP1S1}
     */
    NSPRFLOOKUP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, fromid, lpn,
     * storerkey, sku, screen, loc {@link com.ssaglobal.scm.wms.service.drfmanagement.LotForLPNLookupP1S1}
     */
    NSPRFLOTFORLPNLKUP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sku
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.LotForLPNLookupP1S1}
     */
    NSPRFLOTFORLPNLKUPPICK,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.HoldLotCodeLookupP1S1}
     */
    NSPRFLOTLKUP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, printerID, label,
     * nm1, vl1, nm2, vl2, nm3, vl3, nm4, vl4, nm5, vl5, nm6, vl6, nm7, vl7, nm8, vl8, nm9, vl9, nm10, vl10, nm11,
     * vl11, nm12, vl12, nm13, vl13, nm14, vl14 {@link com.ssaglobal.scm.wms.service.drfmanagement.XRFLabelReprintP1S1}
     */
    NSPRFLP01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, printerid, lpn
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReprintLPNLabelP1S1}
     */
    NSPRFLP02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey,
     * externorderkey, loc, toloc, sku, caseid {@link com.ssaglobal.scm.wms.service.drfmanagement.RFOverPickforSTDP1S1}
     */
    NSPRFLPK01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey,
     * storerkey, fromloc, sku, lpn, uom, qty, packkey, caseid, toloc, pickdetailkey, orderlinenumber
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFOverPickforSTDP2S2}
     */
    NSPRFLPK02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey,
     * fromloc, sku, lpn, uom, qty, packkey, caseid, toloc, csku, clpn, ctyp, cuom, cqty, ccase, dropid, cloc, cfloc,
     * pickdetailkey, taskdetailkey, orderlinenumber, cartongroup, cartontype, currentretrec, transactionkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFOverPickforSTDP3S3}
     */
    NSPRFLPK03,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey, sku
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.LPNForOrderLookupP1S1}
     */
    NSPRFLPNLKUP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, LPN,  loc
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.ValidateLPNReceipt}
     */
    NSPRFLPR1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, receiptkey,
     * storerkey, lot, prokey, sku, pokey, qty, scrnuom, packkey, loc, id, hold, isrp, drid, lottable01, lottable02,
     * lottable03, lottable04, lottable05, lottable06, lottable07, lottable08, lottable09, lottable10, other1,
     * other2, other3, printerID, counter, wgt, reasoncode, RejectQty, PackingSlipQty, lottable11, lottable12,
     * temperature1 {@link com.ssaglobal.scm.wms.service.drfmanagement.RFSingleScanLPNReceiveP1S1}
     */
    NSPRFLPR4,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ID, printerID
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLPNReceiveAll}
     */
    NSPRFLPR6,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loadid,
     * trailerid, door {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLoadSummaryP1S1}
     */
    nspRFLS01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loadid, orderkey,
     * totpallets, ltlrate, printaddlbl, lblprinter, labelcopies, printpacklist, packlistprinter, packlistcopies,
     * printcontentrpt, rptprinter, rptcopies, type, rectype
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.LTLRateP1S1}
     */
    NSPRFLTLRATE3,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loadid, orderkey,
     * totpallets, ltlrate, printaddlbl, lblprinter, labelcopies, printpacklist, packlistprinter, packlistcopies,
     * printcontentrpt, rptprinter, rptcopies, type, rectype
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.LTLRateP1S1}
     */
    NSPRFLTLRATE4,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loadid,
     * externloadid, orderkey, rectype {@link com.ssaglobal.scm.wms.service.drfmanagement.GetPalletsForLTLRatingP1S1}
     */
    NSPRFLTLRATEB,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, dropid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.GetDropIDWeightP1S1}
     */
    NSPRFLTLWGT,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, dropid, stdwgt
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.UpdateDropIDWeightP1S1}
     */
    NSPRFLTLWGT3,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, asnkey,
     * storerkey, sku, fieldname {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLottableMaskLookupP1S1}
     */
    NSPRFLVGMLOOKUP01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, asnkey,
     * storerkey, sku, fieldname {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLottableMaskLookupP1S1}
     */
    NSPRFLVGMLOOKUP02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, asnkey,
     * storerkey, sku, fieldname {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLottableMaskLookupP1S1}
     */
    NSPRFLVGMLOOKUP03,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, asnkey,
     * storerkey, sku, fieldname {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLottableMaskLookupP1S1}
     */
    NSPRFLVGMLOOKUP06,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, asnkey,
     * storerkey, sku, fieldname {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLottableMaskLookupP1S1}
     */
    NSPRFLVGMLOOKUP07,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, asnkey,
     * storerkey, sku, fieldname {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLottableMaskLookupP1S1}
     */
    NSPRFLVGMLOOKUP08,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, asnkey,
     * storerkey, sku, fieldname {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLottableMaskLookupP1S1}
     */
    NSPRFLVGMLOOKUP09,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, asnkey,
     * storerkey, sku, fieldname {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLottableMaskLookupP1S1}
     */
    NSPRFLVGMLOOKUP10,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLaborInstallCheckP1S1}
     */
    NSPRFM1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, type, trankey,
     * chargecode {@link com.ssaglobal.scm.wms.service.drfmanagement.RFGetTransInfoForMiscCharges}
     */
    NSPRFMCH01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, type, trankey,
     * owner, billto, chargecode, createcharge, chargeqty, chargeuom, chargerate, note
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFUpdateMiscCharges}
     */
    NSPRFMCH02A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, type, trankey,
     * sku, chargecode {@link com.ssaglobal.scm.wms.service.drfmanagement.ValidateSOLineOrASNLineChargeCodes}
     */
    NSPRFMCHD01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, type, trankey1,
     * trankey2, owner, billto, chargecode, createcharge, chargeqty, chargeuom, chargerate, note
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFUpdateDetailMiscCharges}
     */
    NSPRFMCHD02A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskkey, equipid,
     * pickdetailkey {@link com.ssaglobal.scm.wms.service.drfmanagement.TMEQTDP1S1}
     */
    NSPRFME2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ScreenName,
     * ProfileName, DeviceName, Locale {@link com.ssaglobal.scm.wms.service.webservices.dao.mobileui.MetaRFScreenDAO}
     */
    NSPRFMETA01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, lang, tokenList
     * {@link com.ssaglobal.scm.wms.service.webservices.dao.mobileui.MetaRFScreenDAO}
     */
    NSPRFMETA02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, device, component
     * {@link com.ssaglobal.scm.wms.service.webservices.dao.mobileui.MetaRFScreenDAO}
     */
    NSPRFMETA03,
    /**
     * Parameters: SENDDELIMITER, PTCID, USERID, TASKID, DATABASENAME, APPFLAG, RECORDTYPE, SERVER, LOCALE, PASSWORD,
     * COMPONENT, TENANT, INSTANCE, VERSION
     * {@link com.ssaglobal.scm.wms.service.webservices.framework.MetaRFDataBasesP1S1}
     */
    NSPRFMETAOT08,
    /**
     * Parameters: SENDDELIMITER, PTCID, USERID, TASKID, DATABASENAME, APPFLAG, RECORDTYPE, SERVER, LOCALE,
     * COMPONENT, TENANT, INSTANCE {@link com.ssaglobal.scm.wms.service.webservices.framework.MetaRFLogout}
     */
    NSPRFMETAOT09,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, fromloc, arg1,
     * arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18,
     * arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.TMPMI01P1S1}
     */
    NSPRFMI1A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toloc,
     * confirmtoloc, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15,
     * arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.TMPMI01P1S2}
     */
    NSPRFMI2A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, fromloc, arg1,
     * arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18,
     * arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30
     * {@link com.ssaglobal.scm.wms.service.dcustomize.RFProdDirectedMoveP1S1}
     */
    NSPRFMI3A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loadid,
     * trailerid, door {@link com.ssaglobal.scm.wms.service.drfmanagement.RFMaintainLoadP1S1}
     */
    NSPRFML01A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFMaintainLoadP2S2}
     */
    NSPRFML01B,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, shipto, loadid,
     * extld, trailerid, type, door, route, carrier, status, deptime, action
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFMaintainLoadP3S3}
     */
    nspRFML02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, fromid, fromloc,
     * lot, allowMoveToLost {@link com.ssaglobal.scm.wms.service.dcustomize.MoveToLostByLotLocId}
     */
    NSPRFMOVETOLOST,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, fromloc, fromid1,
     * fromid2, fromid3, fromid4, fromid5, MultiPalletRemaining
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPutawayMPP1P1S1}
     */
    NSPRFMPP1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey1, sku1,
     * lot1, fromloc1, fromid1, toloc1, toid1, standard, qty1, packkey1, uom1, suggestedtoloc1, finaltoloc1,
     * storerkey2, sku2, lot2, fromloc2, fromid2, toloc2, toid2, standard2, qty2, packkey2, uom2, suggestedtoloc2,
     * finaltoloc2, storerkey3, sku3, lot3, fromloc3, fromid3, toloc3, toid3, standard3, qty3, packkey3, uom3,
     * suggestedtoloc3, finaltoloc3, storerkey4, sku4, lot4, fromloc4, fromid4, toloc4, toid4, standard4, qty4,
     * packkey4, uom4, suggestedtoloc4, finaltoloc4, storerkey5, sku5, lot5, fromloc5, fromid5, toloc5, toid5,
     * standard5, qty5, packkey5, uom5, suggestedtoloc5, finaltoloc5, confirmid, confirmloc
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPutawayMPP2P1S1}
     */
    NSPRFMPP2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, fromid1, fromid2,
     * fromid3, fromid4, fromid5, fromloc1, fromloc2, fromloc3, fromloc4, fromloc5, lot1, lot2, lot3, lot4, lot5,
     * toloc1, toloc2, toloc3, toloc4, toloc5 {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPutawayCancel3P1S1}
     */
    NSPRFMPP2A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * sku, fromloc, fromid, toloc, toid, qty, uom, packkey, refnum, suggestedtoloc, finaltoloc, erryes
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPutaway2P1S1}
     */
    NSPRFMPP3A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, lot, fromloc,
     * toloc, id, finaltoloc {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPutawayCancel2P1S1}
     */
    NSPRFMPP3B,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey1, sku1,
     * lot1, fromloc1, fromid1, toloc1, toid1, qty1, packkey1, uom1, suggestedtoloc1, finaltoloc1, storerkey2, sku2,
     * lot2, fromloc2, fromid2, toloc2, toid2, qty2, packkey2, uom2, suggestedtoloc2, finaltoloc2, storerkey3,
     * sku3, lot3, fromloc3, fromid3, toloc3, toid3, qty3, packkey3, uom3, suggestedtoloc3, finaltoloc3,
     * storerkey4, sku4, lot4, fromloc4, fromid4, toloc4, toid4, qty4, packkey4, uom4, suggestedtoloc4,
     * finaltoloc4, storerkey5, sku5, lot5, fromloc5, fromid5, toloc5, toid5, qty5, packkey5, uom5,
     * suggestedtoloc5, finaltoloc5 {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPutawayMPP4P1S1}
     */
    NSPRFMPP4,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, externasnkey,
     * duns, route {@link com.ssaglobal.scm.wms.service.drfmanagement.RFFindSIDP1S1}
     */
    NSPRFMS01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, externasnkey,
     * duns, route {@link com.ssaglobal.scm.wms.service.drfmanagement.RFFindSIDCP1S1}
     */
    NSPRFMS02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, pickdetailkey,
     * caseid, newcaseid, printerid, newDropID {@link com.ssaglobal.scm.wms.service.drfmanagement.RFMVTONEWCASEIDP1S1}
     */
    NSPRFMVCS,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * sku, fromloc, fromid, toloc, toid, qty, uom, packkey, refnum, var1, var2, var3, actloc, SERIALNUMBER
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFMoveFinalizeBySNP1S1}
     */
    NSPRFMVSN02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttM, ordernumber,
     * oldcaseid, newcaseid, printerid, caseid1, caseid2, caseid3, caseid4, caseid5, caseid6, caseid7, caseid8,
     * caseid9, caseid10, caseid11, caseid12, caseid13, caseid14, caseid15, caseid16, caseid17, caseid18, caseid19,
     * caseid20, caseid21, caseid22, caseid23, caseid24, caseid25, caseid26, caseid27, caseid28, caseid29, caseid30,
     * other1, other2, other3, taskkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFNCI01P1S1}
     */
    NSPRFNCI01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * taskdetailkey, storerkey, sku, fromloc, fromchkdigit, fromid, toloc, tochkdigit, toid, lot, qty, packkey, uom,
     * reason {@link com.ssaglobal.scm.wms.service.drfmanagement.TMOMV01P1S1}
     */
    NSPRFOMV01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * sku, fromid, fromloc, qty, uom, packkey, shiplabel, dropid, toloc, short, cartongroup, cartontype,
     * transactionkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPickOneP1S1}
     */
    NSPRFOP01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, shiplabel,
     * dropid, toloc, cartongroup, cartontype {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPickTwoP1S1}
     */
    NSPRFOP02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, shiplabel,
     * dropid, toloc {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPickThreeP1S1}
     */
    NSPRFOP03,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, caseid, qty,
     * other1, other2, other3, wgt, counter, totlinenumber, opertype, firstkey, lastkey, storerkey, sku,
     * pickdetailkey, id, lot, fromid, fromloc, uom, packkey, shiplabel, dropid, toloc, short, callfrom, totalwgt,
     * transactionkey, prokey, other4, other5 {@link com.ssaglobal.scm.wms.service.drfmanagement.RFOP04P1S1}
     */
    NSPRFOP04,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey, sku,
     * loc, sortby, pickby, repyn {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDynamicPick}
     */
    NSPRFOP06,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, containerid, sku,
     * loc, sortby, pickby, repyn {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDynamicPickContainerID}
     */
    NSPRFOP06C,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, demandkey,
     * orderkey, orderlinenumber, storerkey, sku, loc, id1, id2, id3, qty1, qty2, qty3, toid, stage, stgcheckdigit,
     * door, doorcheckdigit, locid, packkey, uom, printerid, cartongroup, cartontype, opcaller, currentretrec,
     * dropid, transactionkey, toloc, uomcode {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDoDynamicPick}
     */
    NSPRFOP07,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDynPKSkuView}
     */
    NSPRFOP08,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, containerid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDynPKSkuViewContainer}
     */
    NSPRFOP08C,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, demandkey,
     * lastloc {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPickLocs}
     */
    NSPRFOP11,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, demandkey,
     * lastloc {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPickLocs}
     */
    NSPRFOP111,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, demandkey,
     * orderkey, orderlinenumber, storerkey, sku, loc, lpn, id1, qty1, lpn2, id2, qty2, lpn3, id3, qty3, toid, stage,
     * door, locid, packkey, uom, descr {@link com.ssaglobal.scm.wms.service.drfmanagement.RFValidateIds}
     */
    NSPRFOP12,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey, sku,
     * loc, sortby, pickby, repyn {@link com.ssaglobal.scm.wms.service.drfmanagement.RFOP13P1S1}
     */
    NSPRFOP13,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ORDERKEY,
     * totalpallets, totalcases, remapallets, remacases {@link com.ssaglobal.scm.wms.service.drfmanagement.RFOP15P1S1}
     */
    NSPRFOP15,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, containerid,
     * totalpallets, totalcases, remapallets, remacases
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDynContainerIDSummary}
     */
    NSPRFOP15C,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFOPP1S1}
     */
    NSPRFOP20,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loc, sku,
     * priority, orderkey, uom, type {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDynamicPick}
     */
    NSPRFOP210,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loc, sku,
     * priority, orderkey, uom, type {@link com.ssaglobal.scm.wms.service.drfmanagement.RFOverPickforDAP1S1}
     */
    NSPRFOP310,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, lotxidkey,
     * storerkey, sku, qty, firstkey, lastkey, transactionkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFOP41P1S1}
     */
    NSPRFOP41,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loadid, sequence,
     * sku, sortby, uom {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLoadPickforDAP1S1}
     */
    NSPRFOP410,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, lotxidkey,
     * storerkey, sku, qty, firstkey, lastkey, wgt, other1, other2, other3, lotxidlinenumber, currRow, transactionkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFOP43P1S1}
     */
    NSPRFOP43,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, caseid, id,
     * curRec {@link com.ssaglobal.scm.wms.service.drfmanagement.RFGETALLCWRECORDSP1S1}
     */
    NSPRFOP4Y,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * qty, pickdetailkey, fromloc, fromid, toloc, toid, lot, uom, packkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPickMoveP0S0}
     */
    NSPRFOP50,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, key, sku, loc,
     * sortby, UOM, keyType {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDynamicPickLaborP1S1}
     */
    NSPRFOP6AL,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, key, sku, loc,
     * sortby, UOM, keyType {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDynamicPickLaborP1S1}
     */
    NSPRFOP6ALC,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loc, sku,
     * priority, orderkey, uom, externorderkey, toloc, caseid, type, criteriafields
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFOverPickItemList}
     */
    NSPRFOPDALST,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loc, sku,
     * priority, orderkey, uom, externorderkey, toloc, caseid, type, criteriafields
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFOverPickItemList}
     */
    NSPRFOPPDLST,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, assignmentkey,
     * order1, orderid, keyfield, keytype, orderarg1, orderarg2, orderarg3, orderarg4, orderarg5, orderarg6,
     * orderarg7, orderarg8, orderarg9, orderarg10, orderarg11, orderarg12, orderarg13, orderarg14, orderarg15,
     * orderarg16, orderarg17, orderarg18, orderarg19, orderarg20, orderarg21, orderarg22, orderarg23, orderarg24,
     * orderarg25, orderarg26, orderarg27, orderarg28, orderarg29, orderarg30, casearg1, casearg2, casearg3,
     * casearg4, casearg5, casearg6, casearg7, casearg8, casearg9, casearg10, casearg11, casearg12, casearg13,
     * casearg14, casearg15, casearg16, casearg17, casearg18, casearg19, casearg20, casearg21, casearg22, casearg23,
     * casearg24, casearg25, casearg26, casearg27, casearg28, casearg29, casearg30
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFOpenPicksLkupP1S1}
     */
    NSPRFOPPK01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * storerkey, sku, lot, loc, id, qty, yesorno
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.QuantityConfirmP1S1}
     */
    NSPRFOPQCFN,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * storerkey, sku, lot, loc, id, qty, yesorno
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.QuantityConfirmP1S1}
     */
    NSPRFOPQCFY,
    /**
     * {@link com.ssaglobal.scm.wms.service.dcustomize.orderstatus.CalculateAndUpdateOrdersStatus}
     */
    NSPRFORDERSTATUSUPDATER,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loadid, sealno,
     * carrier {@link com.ssaglobal.scm.wms.service.drfmanagement.RFOrderSplitP1S1}
     */
    nspRFOSPL,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * prokey, sku, pokey, qty, uom, packkey, loc, id, lottable01, lottable02, lottable03, lottable04, lottable05,
     * printerID {@link com.ssaglobal.scm.wms.service.drfmanagement.RFOT02P1S1}
     */
    NSPRFOT02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, caseid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFOT03P1S1}
     */
    NSPRFOT03,
    /**
     * Parameters: SENDDELIMITER, PTCID, USERID, TASKID, DATABASENAME, APPFLAG, RECORDTYPE, SERVER, LOCALE, PASSWORD,
     * INSTANCE, VERSION {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDataBasesP1S1}
     */
    NSPRFOT08,
    /**
     * Parameters: SENDDELIMITER, PTCID, USERID, TASKID, DATABASENAME, APPFLAG, RECORDTYPE, SERVER, LOCALE,
     * COMPONENT, INSTANCE {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLogout}
     */
    NSPRFOT09,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey,
     * pickdetailkey, fromid, toloc {@link com.ssaglobal.scm.wms.service.dcrossdock.RFOXDLoadingP0S0}
     */
    NSPRFOXDLD00,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey,
     * pickdetailkey, id {@link com.ssaglobal.scm.wms.service.dcrossdock.RFOXDLoadingP1S1}
     */
    NSPRFOXDLD01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey,
     * pickdetailkey, id, loc, finaltoloc {@link com.ssaglobal.scm.wms.service.dcrossdock.RFOXDLoadingP2S2}
     */
    NSPRFOXDLD02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * prokey, prolinenumber, sku, pokey, qty, uom, packkey, fromloc, fromid, hold, isrp, drid, lottable01,
     * lottable02, lottable03, lottable04, lottable05, lottable06, lottable07, lottable08, lottable09, lottable10,
     * other1, other2, other3, counter, wgt, reasoncode, RejectQty, PackingSlipQty, lottable11, lottable12, toloc,
     * qtyleft, toid, taskdetailkey {@link com.ssaglobal.scm.wms.service.dcrossdock.RFOXDMoveP1S1}
     */
    NSPRFOXDMV01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * prokey, prolinenumber, sku, pokey, qty, uom, packkey, fromloc, fromid, hold, isrp, drid, lottable01,
     * lottable02, lottable03, lottable04, lottable05, lottable06, lottable07, lottable08, lottable09, lottable10,
     * other1, other2, other3, counter, wgt, reasoncode, RejectQty, PackingSlipQty, lottable11, lottable12, toloc,
     * qtyleft, toid {@link com.ssaglobal.scm.wms.service.dcrossdock.RFOXDMoveP1S1}
     */
    NSPRFOXDMV02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * prokey, prolinenumber, sku, pokey, fromqty, uom, packkey, loc, fromid, hold, isrp, drid, lottable01,
     * lottable02, lottable03, lottable04, lottable05, lottable06, lottable07, lottable08, lottable09, lottable10,
     * other1, other2, other3, counter, wgt, reasoncode, RejectQty, PackingSlipQty, lottable11, lottable12, toloc,
     * toqty, id, taskdetailkey {@link com.ssaglobal.scm.wms.service.dcrossdock.RFOXDReceivingP1S1}
     */
    NSPRFOXDRC01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackingP1S1}
     */
    NSPRFP1101,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toid,
     * cartongroup, cartontype, droploc, flag {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackingP2S2}
     */
    NSPRFP11B01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toid, orderkey,
     * fromid, droploc {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackingEntireFromIDOrderP1S1}
     */
    NSPRFP11C01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackingP1S1}
     */
    NSPRFP1201,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toid,
     * cartongroup, cartontype, droploc, flag {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackingP2S2}
     */
    NSPRFP12B01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toid, orderkey,
     * fromid, droploc {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackingEntireFromIDOrderP1S1}
     */
    NSPRFP12C01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackUnPackToIDP1S1}
     */
    NSPRFP1601,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackValidateLPNsP1S1}
     */
    NSPRFP1801,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey, arg1,
     * arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18,
     * arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29, arg30
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackValidateLPNsP2S2}
     */
    NSPRFP18A01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackValidateLPNsP3S3}
     */
    NSPRFP18B01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackingP1S1}
     */
    NSPRFP1A01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toid,
     * cartongroup, cartontype, droploc, flag {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackingP2S2}
     */
    NSPRFP1B01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toid, orderkey,
     * fromid, label1, label2, label3, pckprinterid1, pckcopies1, pckprinterid2, pckcopies2, pckprinterid3,
     * pckcopies3, pckprinterid4, pckcopies4, pckprinterid5, pckcopies5, pckprinterid6, pckcopies6
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackingP3S3}
     */
    NSPRFP301,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toid, orderkey,
     * fromid, sku, lot, qty, pckprinterid1, pckcopies1, pckprinterid2, pckcopies2, pckprinterid3, pckcopies3,
     * pckprinterid4, pckcopies4, pckprinterid5, pckcopies5, pckprinterid6, pckcopies6, transactionkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackingP4S4}
     */
    NSPRFP401,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toid, orderkey,
     * fromid, sku, lot, qty, pckprinterid1, pckcopies1, pckprinterid2, pckcopies2, pckprinterid3, pckcopies3,
     * pckprinterid4, pckcopies4, pckprinterid5, pckcopies5, pckprinterid6, pckcopies6, transactionkey, checkonly
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackingP4S4}
     */
    NSPRFP401S,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toid, orderkey,
     * fromid, sku {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackingLotControlP1S1}
     */
    NSPRFP4ALOT01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toid, orderkey,
     * fromid, sku {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackIDContentsP1S1}
     */
    NSPRFP4FK201,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toid, orderkey,
     * fromid, sku {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackIDContentsP1S1}
     */
    NSPRFP4FK201M,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey, fromid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackFromIDCompleteP1S1}
     */
    NSPRFP4FK301,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey, fromid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackFromIDCompleteP1S1}
     */
    NSPRFP4FK301M,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey, fromid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackFromIDCompleteP2S2}
     */
    NSPRFP4FK3A01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey, fromid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackFromIDCompleteP2S2}
     */
    NSPRFP4FK3A01M,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toid, orderkey,
     * caller {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackToIDCompleteP1S1}
     */
    NSPRFP4FK401,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toid, orderkey,
     * caller {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackToIDCompleteP1S1}
     */
    NSPRFP4FK401M,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toid, orderkey,
     * carrier, stage, door, trackingid, weight, droploc, pckprinterid1, pckcopies1, pckprinterid2, pckcopies2,
     * pckprinterid3, pckcopies3, pckprinterid4, pckcopies4, pckprinterid5, pckcopies5, pckprinterid6, pckcopies6
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackToIDCompleteP2S2}
     */
    NSPRFP4FK4A01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toid, orderkey,
     * carrier, stage, door, trackingid, weight, droploc
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackToIDCompleteP2S2}
     */
    NSPRFP4FK4A01M,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackingNotesP1S1}
     */
    NSPRFP4FK601,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * sku, id, fromloc, qty, uom, packkey, refnum, transactionkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPutaway1P1S1}
     */
    NSPRFPA01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * sku, fromloc, fromid, toloc, toid, qty, uom, packkey, refnum, suggestedtoloc, finaltoloc, erryes,
     * transactionkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPutaway2P1S1}
     */
    NSPRFPA02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, lot, fromloc,
     * toloc, id, finaltoloc {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPutawayCancel2P1S1}
     */
    NSPRFPA02B,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * sku, fromloc, fromid, toloc, toid, qty, uom, packkey, refnum, suggestedtoloc, finaltoloc, erryes
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPutaway2P1S1}
     */
    NSPRFPA02I,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * sku, id, fromloc, qty, uom, packkey, refnum {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPutaway3P1S1}
     */
    NSPRFPA03,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * sku, id, fromloc, qty, uom, packkey, refnum {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPutaway3P1S1}
     */
    NSPRFPA03I,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * sku, id, fromloc, qty, uom, packkey, refnum {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPutaway3P1S1}
     */
    NSPRFPA03L,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, TYPE, CONID
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.ValidIDForPrintP1S1}
     */
    NSPRFPAL,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, IDtype, ID,
     * PrintAddLbl, LblPrinter, Labelcopies, PrintCompliantLbl, CLblPrinter, Clabelcopies, PrintContentRpt,
     * RptPrinter, Rptcopies, num, total {@link com.ssaglobal.scm.wms.service.dlabelprinting.PrintAddrLabelReportP1S1}
     */
    NSPRFPAL2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, IDtype, ID,
     * PrintAddLbl, LblPrinter, Labelcopies, PrintCompliantLbl, CLblPrinter, Clabelcopies, PrintContentRpt,
     * RptPrinter, Rptcopies {@link com.ssaglobal.scm.wms.service.dlabelprinting.SavePrintAddrLabelReportP1S1}
     */
    NSPRFPAL2A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, dropid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.PrintAddLabelReportForDropIDP1S1}
     */
    NSPRFPAL3,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, IDtype, ID,
     * PrintAddLbl, LblPrinter, Labelcopies, PrintCompliantLbl, CLblPrinter, Clabelcopies, PrintContentRpt,
     * RptPrinter, Rptcopies {@link com.ssaglobal.scm.wms.service.dlabelprinting.LoadPrintAddrLabelReportP1S1}
     */
    NSPRFPALA1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, controlkey, team
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPhysicalCountP1S1}
     */
    NSPRFPC1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, controlkey, loc,
     * id, item, team {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPhysicalCountP2S2}
     */
    NSPRFPC2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, controlkey, loc,
     * storerkey, sku, descr, id, qty, uom, team, packkey, tag, pickdetailkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPhysicalCountP3S3}
     */
    NSPRFPC3,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, controlkey,
     * storerkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPhysicalCountValidateStorer}
     */
    NSPRFPC4A1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, controlkey, loc,
     * storerkey, sku, qty, pack, id, uom, Lottable01, Lottable02, Lottable03, Lottable04, Lottable05, Lottable06,
     * Lottable07, Lottable08, Lottable09, Lottable10, Lottable11, Lottable12, team
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.InsertPhysicalCountData}
     */
    NSPRFPC4BI,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, controlkey, loc,
     * item, team {@link com.ssaglobal.scm.wms.service.drfmanagement.RecordEmptyLocation}
     */
    NSPRFPC5,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, controlkey, loc,
     * team {@link com.ssaglobal.scm.wms.service.drfmanagement.UpdateRecordEmptyLocation}
     */
    NSPRFPC6,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loc, storerkey,
     * sku, id {@link com.ssaglobal.scm.wms.service.drfmanagement.PhysicalCountLotAttributes}
     */
    NSPRFPC7,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * taskdetailkey, storerkey, sku, lot, loc, chkdigit, id, qty, packkey, uom, count
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMPcc01P1S1}
     */
    NSPRFPCC01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * currentretrec {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMPcc02P1S1}
     */
    NSPRFPCC02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * id, sku, lot, qty, loc {@link com.ssaglobal.scm.wms.service.dcustomize.RFProdCountAddLPNP1S1}
     */
    NSPRFPCC2A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey,
     * storerkey, sku {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPackConversionDAP1S1}
     */
    NSPRFPCDA,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPackConversionP1S1}
     */
    NSPRFPCDB,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, PackID
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.ValidatePackIDP1S1}
     */
    NSPRFPCK1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, PackID, Orderkey,
     * Lbl1, Lbl2, Lbl3 {@link com.ssaglobal.scm.wms.service.drfmanagement.RFValidateOrdertoPack}
     */
    NSPRFPCK3,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, PackID, Orderkey,
     * SKU {@link com.ssaglobal.scm.wms.service.drfmanagement.RFValidateItemtoPack}
     */
    NSPRFPCK4,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, PackID, Orderkey,
     * SKU, Lot, Lbl1, Lbl2, Lbl3, Qty, Rej, Rsn, QCAuto, Storerkey, QCReq
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFAddItemtoPack}
     */
    NSPRFPCK4A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, PackID
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFAutoUnpackID}
     */
    NSPRFPCK5,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, PackID, SKU, Lot
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFManualUnpackID}
     */
    NSPRFPCK6,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, PackID, Length,
     * Width, Height, Weight {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPackIDComplete}
     */
    NSPRFPCK7,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, PackID, Orderkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPackOrderComplete}
     */
    NSPRFPCK8,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, PackID
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPackIDContents}
     */
    NSPRFPCK9A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, controlkey,
     * physicalcountkey, team, storerkey, sku, loc, id, qty, pack, uom, transactionkey, Lottable01, Lottable02,
     * Lottable03, Lottable04, Lottable05, Lottable06, Lottable07, Lottable08, Lottable09, Lottable10, Lottable11,
     * Lottable12 {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPhysicalCountSerials}
     */
    NSPRFPCS,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, receiptkey,
     * printerid {@link com.ssaglobal.scm.wms.service.drfmanagement.PrintGenericLPNLabels1P1S1}
     */
    NSPRFPGL01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, receiptkey,
     * printerid {@link com.ssaglobal.scm.wms.service.drfmanagement.PrintGenericLPNLabels1P1S1}
     */
    NSPRFPGL01C,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, receiptkey,
     * printerid, palletcount {@link com.ssaglobal.scm.wms.service.drfmanagement.PrintGenericLPNLabels2P1S1}
     */
    NSPRFPGL02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, receiptkey,
     * printerid, palletcount {@link com.ssaglobal.scm.wms.service.drfmanagement.PrintGenericLPNLabels2P1S1}
     */
    NSPRFPGL02C,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * sku, id, loc, qty, uom, packkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPhys1P1S1}
     */
    NSPRFPH01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * sku, id, loc, qty, uom, packkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPhys2P1S1}
     */
    NSPRFPH02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * sku, id, loc, qty, uom, packkey, inventorytag, team
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPhys3P1S1}
     */
    NSPRFPH03,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * loc, qty, uom, packkey, inventorytag, team {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPhys4P1S1}
     */
    NSPRFPH04,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, lot
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMTCCLotLookupP1S1}
     */
    NSPRFPHLOTLKUP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.dcustomize.inventorycount.InventoryCountPhy03CancelP1S1}
     */
    NSPRFPHY03CANCEL,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, serialnumber,
     * locconfirm, taskdetailkey, inventoryid, assignnumber, confirm
     * {@link com.ssaglobal.scm.wms.service.dcustomize.inventorycount.InventoryUnknownP1S1}
     */
    NSPRFPHY03XUNKNOWN,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, serialnumber,
     * locconfirm, taskdetailkey, inventoryid, assignnumber, confirm
     * {@link com.ssaglobal.scm.wms.service.dcustomize.inventorycount.InventoryUnknownP1S1}
     */
    NSPRFPHY03XUNKNOWNB,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, pokey, storerkey,
     * sku, asnkey, lkupcall {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPOASNLookupP1S1}
     */
    NSPRFPOASNLKUP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * externpokey, buyer, supplier, buyerref, supplierref, otherref, externpokey2, potype
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPOLookupP1S1}
     */
    NSPRFPOAUTOLKUP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * externpokey, buyer, supplier, buyerref, supplierref, otherref, externpokey2, potype
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPOLookupP1S1}
     */
    NSPRFPOLKUP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, inventoryqueryid
     * {@link com.ssaglobal.scm.wms.service.dcustomize.inventorycount.PopulateInventoryCountTasks}
     */
    NSPRFPOPINVTASK,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, pokey, storerkey,
     * sku {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPOSkuLookupP1S1}
     */
    NSPRFPOSKULKUP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey,
     * externorderkey, sku {@link com.ssaglobal.scm.wms.service.drfmanagement.ProductionReturnP0S0}
     */
    NSPRFPR01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey, sku,
     * loc, lpn {@link com.ssaglobal.scm.wms.service.drfmanagement.ProductionReturnP1S1}
     */
    NSPRFPR2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey,
     * storerkey, sku, lot, loc, id, packkey, uom, cqty, tqty, aqty, reason, transactionkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberAdjustmentP2S2}
     */
    NSPRFPR3,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loadid, orderkey,
     * externalorderkey, sku, dropid, caseid
     * {@link com.ssaglobal.scm.wms.service.dlabelprinting.RFGenerateOutboundLabels1P1S1}
     */
    NSPRFPROUTLBL1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loadid, orderkey,
     * externalorderkey, sku, dropid, caseid, blank, printed, outerlabels, innerlabels, printerid, copies, printflag
     * {@link com.ssaglobal.scm.wms.service.dlabelprinting.GenerateOutboundLabels1P1S1}
     */
    NSPRFPROUTLBL2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * pokey {@link com.ssaglobal.scm.wms.service.drfmanagement.POItemLookupP1S1}
     */
    NSPRFPS00T,
    /**
     * Parameters: storerkey, sku, lot.fromloc, id.packkey, qty
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.RFPutawayP1S1}
     */
    NSPRFPUTAWAY,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * sku, id, fromloc, toloc, status, qty, uom, packkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCP1S1}
     */
    NSPRFQC01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, LPN
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCInspection}
     */
    NSPRFQC02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, LPN,
     * QtyInspected, QtyRejected, RejectReason, Type, qcdispcode, Status, ReleaseHold, Uom, transactionkey, quarlpn
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCComplete}
     */
    NSPRFQC03,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, StorerKey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.ChkAllowLPNGenerate}
     */
    NSPRFQCALLOWLPNGENERATE,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, StorerKey, pokey,
     * receiptkey, sku, qty, uom, packkey, upc, printerid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.GenerateLPNP1S1}
     */
    NSPRFQCGENLPN,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, receiptkey,
     * storerkey, lot, prokey, sku, pokey, qty, uom, packkey, loc, newid, hold, isrp, drid, lottable01, lottable02,
     * lottable03, lottable04, lottable05, lottable06, lottable07, lottable08, lottable09, lottable10, other1,
     * other2, other3, printerID, counter, wgt, reasoncode, RejectQty, PackingSlipQty, lottable11, lottable12,
     * temperature1, qcorigqty, qcorigid, qcautoflag, qcholdcode, qcautoadjust, qclinenumber, qcprokey, qcloc
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCNewLPNProcessP1S1}
     */
    NSPRFQCLP2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loc, printerid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RCCheckLoc}
     */
    NSPRFRC00C,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, pro, SKU, DESC,
     * EQTY, RCQTY, RMQTY {@link com.ssaglobal.scm.wms.service.drfmanagement.ItemLookupP1S1}
     */
    NSPRFRC00T,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, receiptkey,
     * storerkey, lot, prokey, sku, pokey, qty, uom, packkey, loc, id, hold, isrp, drid, lottable01, lottable02,
     * lottable03, lottable04, lottable05, lottable06, lottable07, lottable08, lottable09, lottable10, other1,
     * other2, other3, printerID, counter, wgt, reasoncode, RejectQty, PackingSlipQty, lottable11, lottable12,
     * temperature1, transactionkey, usr1, usr2, usr3, usr4, usr5
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReceiveP1S1}
     */
    NSPRFRC01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, receiptkey,
     * storerkey, lot, prokey, sku, pokey, qty, uom, packkey, loc, id, hold, isrp, drid, lottable01, lottable02,
     * lottable03, lottable04, lottable05, lottable06, lottable07, lottable08, lottable09, lottable10, other1,
     * other2, other3, printerID, counter, wgt, reasoncode, RejectQty, PackingSlipQty, lottable11, lottable12,
     * temperature1, transactionkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReceiveP1S1}
     */
    NSPRFRC01A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, receiptkey,
     * storerkey, lot, prokey, sku, pokey, qty, uom, packkey, loc, id, hold, isrp, drid, lottable01, lottable02,
     * lottable03, lottable04, lottable05, lottable06, lottable07, lottable08, lottable09, lottable10, other1,
     * other2, other3, printerID, counter, wgt, reasoncode, RejectQty, PackingSlipQty, lottable11, lottable12,
     * temperature1, transactionkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReceiveP1S1}
     */
    NSPRFRC01BA,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, receiptkey,
     * storerkey, lot, prokey, sku, pokey, qty, uom, packkey, loc, id, hold, isrp, drid, lottable01, lottable02,
     * lottable03, lottable04, lottable05, lottable06, lottable07, lottable08, lottable09, lottable10, other1,
     * other2, other3, printerID, counter, wgt, reasoncode, RejectQty, PackingSlipQty, lottable11, lottable12,
     * transactionkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReceiveP1S1}
     */
    NSPRFRC01E,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, receiptkey,
     * storerkey, lot, prokey, sku, pokey, qty, uom, packkey, loc, id, hold, isrp, drid, lottable01, lottable02,
     * lottable03, lottable04, lottable05, lottable06, lottable07, lottable08, lottable09, lottable10, other1,
     * other2, other3, printerID, counter, wgt, reasoncode, RejectQty, PackingSlipQty, lottable11, lottable12,
     * temperature1, fromcc, transactionkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReceiveP1S1}
     */
    NSPRFRC01N,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, receiptkey,
     * storerkey, lot, prokey, sku, pokey, qty, uom, packkey, loc, id, hold, isrp, drid, lottable01, lottable02,
     * lottable03, lottable04, lottable05, lottable06, lottable07, lottable08, lottable09, lottable10, other1,
     * other2, other3, printerID, counter, wgt, reasoncode, RejectQty, PackingSlipQty, lottable11, lottable12,
     * temperature1, transactionkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReceiveP1S1}
     */
    NSPRFRC01P,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, receiptkey,
     * storerkey, lot, prokey, sku, pokey, qty, uom, packkey, loc, id, hold, isrp, drid, lottable01, lottable02,
     * lottable03, lottable04, lottable05, lottable06, lottable07, lottable08, lottable09, lottable10, other1,
     * other2, other3, printerID, counter, wgt, reasoncode, RejectQty, PackingSlipQty, lottable11, lottable12,
     * temperature1, transactionkey, usr1, usr2, usr3, usr4, usr5, isManualSetupRequired, lpnlabels
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReceiveP1S1}
     */
    NSPRFRC01PL,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, receiptkey,
     * storerkey, lot, prokey, sku, pokey, qty, uom, packkey, loc, id, hold, isrp, drid, lottable01, lottable02,
     * lottable03, lottable04, lottable05, lottable06, lottable07, lottable08, lottable09, lottable10, other1,
     * other2, other3, printerID, counter, wgt, reasoncode, RejectQty, PackingSlipQty, lottable11, lottable12,
     * temperature1, transactionkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReceiveP1S1}
     */
    NSPRFRC01R,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, receiptkey,
     * storerkey, lot, prokey, sku, pokey, qty, uom, packkey, loc, id, hold, isrp, drid, lottable01, lottable02,
     * lottable03, lottable04, lottable05, lottable06, lottable07, lottable08, lottable09, lottable10, other1,
     * other2, other3, printerID, counter, wgt, reasoncode, RejectQty, PackingSlipQty, lottable11, lottable12,
     * temperature1, transactionkey, usr1, usr2, usr3, usr4, usr5, isManualSetupRequired, needprint
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReceiveP1S1}
     */
    NSPRFRC01RET,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, receiptkey,
     * storerkey, lot, prokey, sku, pokey, qty, uom, packkey, loc, id, hold, isrp, drid, lottable01, lottable02,
     * lottable03, lottable04, lottable05, lottable06, lottable07, lottable08, lottable09, lottable10, other1,
     * other2, other3, printerID, counter, wgt, reasoncode, RejectQty, PackingSlipQty, lottable11, lottable12,
     * temperature1, transactionkey, usr1, usr2, usr3, usr4, usr5, isManualSetupRequired
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReceiveP1S1}
     */
    NSPRFRC01X,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, printerID, PO,
     * ASN, Storer, Sku, QTY, Pack, UOM {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReceivePTB1P1S1}
     */
    NSPRFRC021,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, printerID, PO,
     * ASN, Storer, Sku, Pack, UOM {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReceivePTB2P1S1}
     */
    NSPRFRC022,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, printerID, PO,
     * ASN {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReceivePTB3P1S1}
     */
    NSPRFRC023,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ASNKey, id,
     * rectype {@link com.ssaglobal.scm.wms.service.dcustomize.RFClosePLReceiptP1S1}
     */
    NSPRFRC02PL,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey,
     * prokey, prolinenumber, sku, lot, qty, loc, uom, packkey, id, other1, other2, other3, wgt, counter,
     * rcv_counter, totallinenumber, lotxidkey, OperType, firstkey, lastkey, receiptkey, pokey, hold, isrp, drid,
     * lottable01, lottable02, lottable03, lottable04, lottable05, lottable06, lottable07, lottable08, lottable09,
     * lottable10, printerID, totalwgt, lottable11, lottable12, other4, other5, cur_counter, processtype, trantype,
     * itrnkey, lot, action, transactionkey, vsku, f7option
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFRC03P1S1}
     */
    NSPRFRC03,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, caseid, lpn,
     * orderkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReceiptReversalP1S1}
     */
    NSPRFRC04,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, permchk
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.CheckRRPersmissionP1S1}
     */
    NSPRFRC04P,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * qty, lotxidkey, totallinenumber, firstkey, lastkey, altsku, transactionkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFRC06P1S1}
     */
    NSPRFRC06,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, selector
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFRC07P1S1}
     */
    NSPRFRC07,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * qty, lotxidkey, totallinenumber, firstkey, lastkey, snum, other1, other2, wgt, LotxIDLineNumber, currRow
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFRC08P1S1}
     */
    NSPRFRC08,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFCheckQuantityInputEnabled}
     */
    NSPRFRC10QTYENABLED,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, pro, storerkey,
     * SKU, DESC, EQTY, RCQTY, RMQTY {@link com.ssaglobal.scm.wms.service.drfmanagement.ItemLookupP1S1}
     */
    NSPRFRC10T,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sku, storerkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFCheckManualSetupRequiredP1S1}
     */
    NSPRFRC10XXX,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, transshipid,
     * customer, vendor, doc, qty, currentloc, weight, cube, printerID
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReceiveTransshipP1S1}
     */
    NSPRFRC13,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sku, storerkey,
     * tag, loc {@link com.ssaglobal.scm.wms.service.drfmanagement.RFUpdateManualSetupRequiredP1S1}
     */
    NSPRFRC15,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * sourcekey, sourcelinenumber, lot, loc, fromloc, toloc, id, fromid, caseid, dropid, uom1, packkey1, uom2,
     * packkey2, qty, pickdetailkey, processtype, trantype, itrnkey, lot, action, transactionkey, oqty
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberCancelP1S1}
     */
    NSPRFRC300,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * sourcekey, sourcelinenumber, lot, loc, fromloc, toloc, id, fromid, caseid, dropid, uom1, packkey1, uom2,
     * packkey2, qty, pickdetailkey, processtype, trantype, itrnkey, lot, action, transactionkey, oqty
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberCancelP1S1}
     */
    NSPRFRC301,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, pro,
     * confirmClose, isFirstTimeExecution, isDamageCheck, toid
     * {@link com.ssaglobal.scm.wms.service.dcustomize.ReceivedBOMChecker}
     */
    NSPRFRCBOMCHECK,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, receiptkey,
     * storerkey, lot, prokey, sku, pokey, qty, uom, packkey, loc, id, hold, isrp, drid, lottable01, lottable02,
     * lottable03, lottable04, lottable05, lottable06, lottable07, lottable08, lottable09, lottable10, other1,
     * other2, other3, printerID, counter, wgt, reasoncode, RejectQty, Receiptlinenumber, Currentpalletid,
     * newpalletid, lpndetailkey, lottable11, lottable12, temperature1, visitemp
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReceiveCase}
     */
    NSPRFRCC001,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, Type, ASNKey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.GetASNType}
     */
    NSPRFRCC01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, Type, ASNKey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.GetASNType}
     */
    NSPRFRCC01A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, Type, ASNKey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.GetASNType}
     */
    NSPRFRCC01BA,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, id
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDropP1S1}
     */
    NSPRFRCC01E,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, Type, ASNKey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.GetASNType}
     */
    NSPRFRCC01K,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, Type, ASNKey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.GetASNType}
     */
    NSPRFRCC01N,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, Type, ASNKey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.GetASNType}
     */
    NSPRFRCC01P,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, Type, ASNKey,
     * ExternASNKey, palletcount {@link com.ssaglobal.scm.wms.service.drfmanagement.GetASNType}
     */
    NSPRFRCC01PL,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, Type, ASNKey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.GetASNType}
     */
    NSPRFRCC01R,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ASNKey, ID
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.ValidateLPNPalletID}
     */
    NSPRFRCC02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ASNKey, ID, SKU,
     * Qty, scanID {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReceiveAll}
     */
    NSPRFRCC04,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, Typ, doc, pro,
     * id, loc, lpnlabels {@link com.ssaglobal.scm.wms.service.dcustomize.CheckIdOnReceiving}
     */
    NSPRFRCC06PL,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * pro {@link com.ssaglobal.scm.wms.service.drfmanagement.GetSKUInfoP1S1}
     */
    NSPRFRCCGSI,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, doc
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFValidateASNP1S1}
     */
    NSPRFRCCX01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, doc, owner, type,
     * supplier, carrier {@link com.ssaglobal.scm.wms.service.drfmanagement.RFValidateASNHeader1P1S1}
     */
    NSPRFRCCX02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, doc, extrcpt, po,
     * supplier, carrier, carref, vehicle, trailer, rma, packlst
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.ReceiptHeaderLookupP1S1}
     */
    NSPRFRCCXS1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, fromid, toid
     * {@link com.ssaglobal.scm.wms.service.dcustomize.CheckIdInput}
     */
    NSPRFRCIDINPUT,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, POKey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFGetPOInfoP1S1}
     */
    NSPRFRCP01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, POKey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFGetPOInfoP1S1}
     */
    NSPRFRCP01A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * pro, pokey, qty, rejectqty, uom, packkey, id, hold, isrp, putzone, user1, user2, user3, user4, user5,
     * lottable01, lottable02, lottable03, lottable04, lottable05, lottable06, lottable07, lottable08, lottable09,
     * lottable10, lottable11, lottable12, other1, temperature1, transactionkey, lkupcall
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFPOSkuAutoPopulateP1S1}
     */
    NSPRFRCP02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ReceiptKey,
     * EXTERNRECEIPTKEY, storerkey, type, po, supplier, carrier, carref, vehicle, trailer, rma, packlst, wareref,
     * contref, grn, grndate, trlrown, dvrname, actualshipdate, temperature, udf1, udf2, udf3, udf4, udf5
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFCreateASNHeaderP1S1}
     */
    NSPRFRCPTHDR,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, receiptkey,
     * EXTERNRECEIPTKEY, storerkey, type, po, supplier, carrier, carref, vehicle, trailer, rma, packlst, wareref,
     * contref, grn, grndate, trlrown, dvrname, actualshipdate, temperature, udf1, udf2, udf3, udf4, udf5
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFUpdateASNHeaderP1S1}
     */
    NSPRFRCPTUPH,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * template, dayExp, monthExp, lot4, lot5 {@link com.ssaglobal.scm.wms.service.dcustomize.SetExpiredSettings}
     */
    NSPRFRCSHLF02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sku, orderkey,
     * pshka {@link com.ssaglobal.scm.wms.service.dcustomize.SearchSerialForReturns}
     */
    NSPRFRCSRCHSER,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, hold, loc, tag,
     * receiptkey {@link com.ssaglobal.scm.wms.service.dcustomize.SetLottableForHold}
     */
    NSPRFRCSTRET,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, uid, printerid
     * {@link com.ssaglobal.scm.wms.service.dcustomize.RFReprintUIDLabelsP1S1}
     */
    NSPRFREPRINTLBL,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, LPN, SKU
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReturns}
     */
    NSPRFRET01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, LPN, SKU,
     * ReturnType, ReturnReason, ReturnCondition, DispositionType, DispositionCode
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReturnsUpdate}
     */
    NSPRFRET02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, serialnumber,
     * isLotDublicated {@link com.ssaglobal.scm.wms.service.drfmanagement.RFGSIBySerialNumber}
     */
    NSPRFRETGSI01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * sku, fromloc, fromid, toloc, toid, qty, uom, packkey, refnum, var1, var2, var3, transactionkey, loc
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFMoveToIntransitP1S1}
     */
    NSPRFRL01A1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * sku, fromloc, fromid, toloc, toid, qty, uom, packkey, refnum, var1, var2, var3
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFMoveToIntransitP1S1}
     */
    NSPRFRL01A1D,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sku, lot,
     * fromloc, fromid, qty, loc {@link com.ssaglobal.scm.wms.service.dutilitymanagement.ValidateMoveData}
     */
    NSPRFRL01A1V,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * sku, fromloc, fromid, toloc, toid, qty, uom, packkey, refnum, var1, var2, var3, actloc
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFMoveFinalizeP1S1}
     */
    NSPRFRL01B1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * sku, fromloc, fromid, toloc, toid, qty, uom, packkey, refnum, var1, var2, var3
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFMoveToIntransitP1S1}
     */
    NSPRFRL01LPA,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * sku, fromloc, fromid, toloc, toid, qty, uom, packkey, refnum, var1, var2, var3
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFMoveToIntransitP1S1}
     */
    NSPRFRL01LPAD,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, lot,
     * sku, fromloc, fromid, toloc, toid, qty, uom, packkey, refnum, var1, var2, var3, actloc
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFMoveFinalizeP1S1}
     */
    NSPRFRL01LPB,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, MovableUnit,
     * caseid, storer, lotnum, sku, desc, fromtag, fromloc, totag, toloc, toqty, uom, packkey, refnum
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFMoveFlowThroughP1S1}
     */
    NSPRFRL02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, MovableUnit
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFMoveFlowThroughQP1S1}
     */
    NSPRFRL021,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, from,
     * MovableUnit, caseid, storer, lotnum, sku, desc, fromtag, fromloc, totag, toqty, uom, packkey, refnum, loc,
     * door, stage, pack, contoloc, toloc {@link com.ssaglobal.scm.wms.service.drfmanagement.RFMoveFlowValidateToLoc}
     */
    NSPRFRL02A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, containerID,
     * fromloc, toloc, completed, verify {@link com.ssaglobal.scm.wms.service.drfmanagement.RFMoveTransshipP1S1}
     */
    NSPRFRL03,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, containerID
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFMoveTransshipQP1S1}
     */
    NSPRFRL031,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, fromid
     * {@link com.ssaglobal.scm.wms.service.dcustomize.GetNextReplenishmentOrderTaskForId}
     */
    NSPRFRO05,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, serialnumber,
     * toid, fromid, lot, toloc {@link com.ssaglobal.scm.wms.service.dcustomize.CompleteReplenishmentOrderTask}
     */
    NSPRFRO06,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, fromid, lot
     * {@link com.ssaglobal.scm.wms.service.dcustomize.SkipLotAtReplenishmentOrderTask}
     */
    NSPRFRO07,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, fromid, toloc,
     * isFullPalletMove {@link com.ssaglobal.scm.wms.service.dcustomize.ROReturnIDToLoc}
     */
    NSPRFRO08,
    /**
     * Parameters: senddelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, zone01, zone02,
     * zone03, zone04, zone05, zone06, zone07, zone08, zone09, zone10, zone11, zone12
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFRepl1P1S1}
     */
    NSPRFRP01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskindicator
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFRepl2P1S1}
     */
    NSPRFRP02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server,
     * replenishmentgroup, replenishmentkey, storerkey, lot, fromsku, sku, fromloc, fromchkdigit, fromid, toloc,
     * finaltoloc, tochkdigit, toid, fromqty, qty, uom, packkey, itrnkey, transactionkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFRepl3P1S1}
     */
    NSPRFRP03,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, replkey, loc,
     * toloc, reason {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReplnMoveToFinalP1S1}
     */
    NSPRFRP03B,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFRepl4P1S1}
     */
    NSPRFRP04,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loadid, orderkey,
     * externalorderkey, sku, dropid, caseid, labelid, reprint
     * {@link com.ssaglobal.scm.wms.service.dlabelprinting.ReprintOutboundLabels1P1S1}
     */
    NSPRFRPOUTLBL1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, loadid, orderkey,
     * externalorderkey, sku, dropid, caseid, labelid, reprint, blank, printed, outerlabels, innerlabels, printerid,
     * copies {@link com.ssaglobal.scm.wms.service.dlabelprinting.PrintOutboundLabels1P1S1}
     */
    NSPRFRPOUTLBL2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, fromid,
     * allowCloseCart, lot {@link com.ssaglobal.scm.wms.service.dcustomize.ShortSerails}
     */
    NSPRFRROSHORTTASKS,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm, fromid,
     * fromloc, toid, toloc, action, confirm, carrierid, cartongroup, cartontype, islabelsshipped
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.ServiceSHP1S1}
     */
    NSPRFSH01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm, fromid,
     * fromloc, toid, toloc, action, confirm, carrierid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.ServiceSHP1S1}
     */
    NSPRFSH120,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm, fromid,
     * fromloc, toid, toloc, action, confirm, carrierid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.ServiceSHP1S1}
     */
    NSPRFSH130,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, type, key
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.ShipFromShipToLookupP1S1}
     */
    NSPRFSHIPFROMTOLKUP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * sourcekey, sourcelinenumber, lot, loc, fromloc, toloc, id, fromid, caseid, dropid, uom1, packkey1, uom2,
     * packkey2, qty, pickdetailkey, other2, other3, other4, other5, printerid, totalwgt, counter, rcv_counter,
     * totallinenumber, data1, wgt1, data2, wgt2, data3, wgt3, data4, wgt4, data5, wgt5, data6, wgt6, data7, wgt7,
     * data8, wgt8, data9, wgt9, data10, wgt10, processtype, trantype, itrnkey, lot, action, transactionkey, oqty
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.SerialNumberTrackingP2S2}
     */
    NSPRFSM01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * sourcekey, sourcelinenumber, lot, loc, fromloc, toloc, id, fromid, caseid, dropid, uom1, packkey1, uom2,
     * packkey2, qty, pickdetailkey, other2, other3, other4, other5, printerid, totalwgt, counter, rcv_counter,
     * totallinenumber, data1, wgt1, data2, wgt2, data3, wgt3, data4, wgt4, data5, wgt5, data6, wgt6, data7, wgt7,
     * data8, wgt8, data9, wgt9, data10, wgt10, processtype, trantype, itrnkey, lot, action, transactionkey, oqty
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.SerialNumberTrackingP2S2}
     */
    NSPRFSM02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * sourcekey, sourcelinenumber, lot, loc, fromloc, toloc, id, fromid, caseid, dropid, uom1, packkey1, uom2,
     * packkey2, qty, pickdetailkey, other2, other3, other4, other5, printerid, totalwgt, data1, wgt1, data2, wgt2,
     * data3, wgt3, data4, wgt4, data5, wgt5, data6, wgt6, data7, wgt7, data8, wgt8, data9, wgt9, data10, wgt10,
     * processtype, trantype, itrnkey, lot, action, order9, TYP, transactionkey, oqty
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.DetermineEndtoEndCatchWeight}
     */
    NSPRFSM10,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * sourcekey, sourcelinenumber, lot, loc, fromloc, toloc, id, fromid, caseid, dropid, uom1, packkey1, uom2,
     * packkey2, qty, pickdetailkey, processtype, trantype, itrnkey, lot, action, transactionkey, oqty
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberCancelP1S1}
     */
    NSPRFSM101,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * sourcekey, sourcelinenumber, lot, loc, fromloc, toloc, id, fromid, caseid, dropid, uom1, packkey1, uom2,
     * packkey2, qty, pickdetailkey, other2, other3, other4, other5, printerid, totalwgt, data1, wgt1, data2, wgt2,
     * data3, wgt3, data4, wgt4, data5, wgt5, data6, wgt6, data7, wgt7, data8, wgt8, data9, wgt9, data10, wgt10,
     * processtype, trantype, itrnkey, lot, action, order9, TYP, transactionkey, oqty
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.DetermineEndtoEndCatchWeight}
     */
    NSPRFSM20,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * sourcekey, sourcelinenumber, lot, loc, fromloc, toloc, id, fromid, caseid, dropid, uom1, packkey1, uom2,
     * packkey2, qty, pickdetailkey, processtype, trantype, itrnkey, lot, action, transactionkey, oqty, screen
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberCancelP1S1}
     */
    NSPRFSM201,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * sourcekey, sourcelinenumber, lot, loc, fromloc, toloc, id, fromid, caseid, dropid, uom1, packkey1, uom2,
     * packkey2, qty, pickdetailkey, processtype, trantype, itrnkey, lot, action, sncaller, transactionkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberCancelP2S2}
     */
    NSPRFSM202,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * pro, prolinenumber, lot, loc, fromloc, toloc, tag, lpn, fromid, id, toid, dropid, uom, packkey, qty, tqty,
     * pickdetailkey, processtype, transactionkey, order1, order2, order3, order4, order5, order6, order7, order8,
     * order9, qtyfield, screen, demandkey, qtytopick, pickedqty, storerkey2, sku2, uom2, packkey2, aqty, rej_cfm,
     * typ, order11, order12 {@link com.ssaglobal.scm.wms.service.dutilitymanagement.SerialNumberGatewayP1S1}
     */
    NSPRFSMC1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * pro, prolinenumber, lot, loc, fromloc, toloc, tag, lpn, fromid, id, toid, dropid, uom, packkey, qty, tqty,
     * pickdetailkey, processtype, transactionkey, order1, order2, order3, order4, order5, order6, order7, order8,
     * order9, qtyfield, screen, demandkey, qtytopick, pickedqty, storerkey2, sku2, uom2, packkey2, f7option
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.SerialNumberGatewayP1S1}
     */
    NSPRFSMC10,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * transactionkey, oqty, qtyfield {@link com.ssaglobal.scm.wms.service.dutilitymanagement.SerialNumberSetQtyP1S1}
     */
    NSPRFSMC11,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, order1, order9,
     * altsku, transactionKey {@link com.ssaglobal.scm.wms.service.dcustomize.SerialNumberAppender}
     */
    NSPRFSMC1X,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey,
     * prokey, sku, transactionkey, qty, loc, lpn, lottable01, lottable02, lottable03, lottable04, lottable05,
     * lottable06, lottable07, lottable08, lottable09, lottable10, lottable11, lottable12, serialnumber, hold
     * {@link com.ssaglobal.scm.wms.service.dcustomize.GenerateSerialNumber}
     */
    NSPRFSMGEN1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * qty, qtyfield, order9, order1, transactionkey, processtype, loc
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFSerialTempCheckP1S1}
     */
    NSPRFSMX1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey,
     * prokey, prolinenumber, sku, lot, qty, loc, uom, packkey, id, other1, other2, other3, iwgt, owgt, counter,
     * rcv_counter, totallinenumber, lotxidkey, OperType, firstkey, lastkey, receiptkey, pokey, hold, isrp, drid,
     * lottable01, lottable02, lottable03, lottable04, lottable05, lottable06, lottable07, lottable08, lottable09,
     * lottable10, printerID, totalwgt, lottable11, lottable12, other4, other5, cur_counter, processtype, trantype,
     * itrnkey, lot, action, pickdetailkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFSN03P1S1}
     */
    NSPRFSN03,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, FUNCTION, SER,
     * OWNR, SKU, LPN, ASN, ORDER, PICKKEY, LINE
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberMaintainanceP1S1}
     */
    NSPRFSN1A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * sourcekey, sourcelinenumber, lot, loc, fromloc, toloc, id, fromid, caseid, dropid, uom1, packkey1, uom2,
     * packkey2, qty, pickdetailkey, printerid, totalwgt, counter, rcv_counter, totallinenumber, processtype,
     * trantype, itrnkey, lot, action, identifier, barcodeScanStr, transactionkey
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.RFSerialBarcodeParserP1S1}
     */
    NSPRFSNBRCD01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, userkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberRecoveryP0S0}
     */
    NSPRFSND00,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, userkey, id, loc
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberRecoveryP1S1}
     */
    NSPRFSND01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, userkey, id, loc
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberRecoveryP1S1}
     */
    NSPRFSND01A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, processtype, key,
     * lineno, pickdetailkey, storerkey, sku, lot, loc, id, qty, snqty, remqty, itrnkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberRecoveryP2S2}
     */
    NSPRFSND02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, itrnkey, invqty,
     * snqty {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberCloseP1S1}
     */
    NSPRFSNDCL01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, serialnumber,
     * storerkey, sku, id, loc {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberInquiryP1S1}
     */
    NSPRFSNI01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * id, loc, SER, data2, data3, data4, data5, WEIGHT, serialkey, currentrow
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberMaintainanceP1S3}
     */
    NSPRFSNIU1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sku, id, loc
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberInquiryP2S2}
     */
    NSPRFSNL01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sku, id, loc
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberInquiryP2S2a}
     */
    NSPRFSNL01A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sku, id, loc
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberInquiryIDLookup}
     */
    NSPRFSNL01IDLKUP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sku, id, loc
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberInquiryLocLookup}
     */
    NSPRFSNL01LOCLKUP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sku, id, loc
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberInquirySKULookup}
     */
    NSPRFSNL01SKULKUP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * lot, itrnkey, transactionkey {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberLookupP1S1}
     */
    NSPRFSNLST01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey, sku, id
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberPickRecoveryP1S1}
     */
    NSPRFSNMP1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, receiptkey, sku,
     * id {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberReceiptRecoveryP1S1}
     */
    NSPRFSNMR1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * id, loc {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberMoveRecoveryP1S1}
     */
    NSPRFSNMV1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, itrnkey
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.SerialTempResetP1S1}
     */
    NSPRFSNRS1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, resetuser
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.SerialTempResetP1S1}
     */
    NSPRFSNRS2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, pickdetailkey,
     * lpn, qty {@link com.ssaglobal.scm.wms.service.drfmanagement.RFSplitPickDetail}
     */
    NSPRFSPD03,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, pickdetailkey,
     * lpn, qty {@link com.ssaglobal.scm.wms.service.drfmanagement.SplitPickDetailHelper}
     */
    NSPRFSPDH,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey,
     * externorderkey, sku, caseid, id {@link com.ssaglobal.scm.wms.service.drfmanagement.PickDetailSearch}
     */
    NSPRFSPDX,
    /**
     * Parameters: printerid, transactiontype, id, idtype, scaccode, servicetype, copies, weight, height, length,
     * width, orderkey, source,  strategykey {@link com.ssaglobal.scm.wms.service.spsintegration.SPSHelper}
     */
    NSPRFSPS,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * sourcekey, sourcelinenumber, lot, loc, fromloc, toloc, id, fromid, caseid, dropid, uom1, packkey1, uom2,
     * packkey2, qty, pickdetailkey, other2, other3, other4, other5, printerid, totalwgt, counter, rcv_counter,
     * totallinenumber, range1, range2, processtype, trantype, itrnkey, lot, action, transactionkey
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.SerialNumberTrackingP3S3}
     */
    NSPRFSR01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * sourcekey, sourcelinenumber, lot, loc, fromloc, toloc, id, fromid, caseid, dropid, uom1, packkey1, uom2,
     * packkey2, qty, pickdetailkey, other2, other3, other4, other5, printerid, totalwgt, counter, rcv_counter,
     * totallinenumber, range1, range2, processtype, trantype, itrnkey, lot, action, transactionkey
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.SerialNumberTrackingP3S3}
     */
    NSPRFSR02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, SER, data2,
     * data3, data4, data5, WEIGHT, serialkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.SerialNumberMaintainanceP1S2}
     */
    NSPRFSU2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, assignment, flag,
     * eqmtid, eqmttype, bldclusterflag, dropid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLaborAssignmentTasksP1S1}
     */
    NSPRFTASSG,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, assignment, flag,
     * eqmtid, eqmttype, bldclusterflag {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLaborAssignmentTasksP1S1}
     */
    NSPRFTASSG1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, assignment, flag
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLaborAssignmentCancelP1S1}
     */
    NSPRFTASSGB,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, assignment, flag
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLaborAssignmentCancelP1S1}
     */
    NSPRFTASSGB1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, assignment, flag,
     * fromlocold {@link com.ssaglobal.scm.wms.service.drfmanagement.RFLaborAssignmentTasksP1S1}
     */
    NSPRFTASSGPK01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * taskdetailkey, storerkey, sku, lot, loc, chkdigit, id, qty, packkey, uom
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMtcc01P1S1}
     */
    NSPRFTCC01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * taskdetailkey, storerkey, sku, lot, loc, chkdigit, id, qty, packkey, uom, itrnkey, transactionkey
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMtcc02P1S1}
     */
    NSPRFTCC02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * taskdetailkey, storerkey, sku, lot, fromloc, fromid, qty, packkey, uom
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMtcc03P1S1}
     */
    NSPRFTCC03,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, lot
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMTCCLotLookupP1S1}
     */
    NSPRFTCCLOTLKUP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm, area01,
     * taskdetailkey, toid, toid1, toid2, toid3, toid4, intransitloc
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTCM01P1S1}
     */
    NSPRFTCM01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * taskdetailkey1, taskdetailkey2, taskdetailkey3, taskdetailkey4, toid, toid1, toid2, toid3, toid4
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTCM02P1S1}
     */
    NSPRFTCM02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * toid, storerkey, sku, fromloc, fromchkdigit, fromid, toloc, lot, qty, tag, packkey, uom, reason, itrnkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTCM03P1S1}
     */
    NSPRFTCM03,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * toid, toloc {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTCM05P1S1}
     */
    NSPRFTCM05,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * taskdetailkey1, taskdetailkey2, taskdetailkey3, taskdetailkey4, toid, toloc, toid1, toloc1, toid2, toloc2,
     * toid3, toloc3, toid4, toloc4, ctoid, cloc {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTTCM06P1S1}
     */
    NSPRFTCM06,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * taskdetailkey1, taskdetailkey2, taskdetailkey3, taskdetailkey4, toid, toloc, toid1, toloc1, toid2, toloc2,
     * toid3, toloc3, toid4, toloc4, ctoid, cloc {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTTCM06F2P1S1}
     */
    NSPRFTCM06F2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * taskdetailkey1, taskdetailkey2, taskdetailkey3, taskdetailkey4, toid, toloc, toid1, toloc1, toid2, toloc2,
     * toid3, toloc3, toid4, toloc4, ctoid, cloc {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTTCM6F2P1S1}
     */
    NSPRFTCM6F2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toid, toid1,
     * toid2, toid3, toid4, toid5, toid6, toid7, toid8, toid9
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTTCMMV01P1S1}
     */
    NSPRFTCMMV01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toid, toloc,
     * toid1, toloc1, toid2, toloc2, toid3, toloc3, toid4, toloc4, toid5, toloc5, toid6, toloc6, toid7, toloc7,
     * toid8, toloc8, toid9, toloc9, ctoid, cloc {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTTCMMV02P1S1}
     */
    NSPRFTCMMV02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm, area01,
     * taskdetailkey, toid, operator, priority {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTCR01P1S1}
     */
    NSPRFTCR01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * toid {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTCR02P1S1}
     */
    NSPRFTCR02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * toid, storerkey, sku, fromloc, fromchkdigit, fromid, toloc, lot, qty, tag, packkey, uom, reason, itrnkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTCR03P1S1}
     */
    NSPRFTCR03,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * toid, toloc {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTCR04P1S1}
     */
    NSPRFTCR04,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * toid, toloc {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTCR05P1S1}
     */
    NSPRFTCR05,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskkey,
     * pickdetailkey {@link com.ssaglobal.scm.wms.service.drfmanagement.TMEVTDP1S1}
     */
    NSPRFTD00,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskkey,
     * pickdetailkey, from, fromloc, fromid, space1, space2, loc, door, stage, pack, space3, contoloc, confirmloc
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTTDP1S1}
     */
    NSPRFTD01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskkey,
     * pickdetailkey, from, fromloc, fromid, space1, space2, loc, door, stage, pack, space3, contoloc, confirmloc,
     * userconfirmed {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTTDP1S1}
     */
    NSPRFTD01A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskkey, from,
     * lastloc, equipid, idcount, loc, door, stage, pack, contoloc, confirmloc, confirmid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.TMEQTDP1S2}
     */
    NSPRFTD02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskkey, equipid,
     * confirmloc, confirmid {@link com.ssaglobal.scm.wms.service.drfmanagement.TMEQTDP1S3}
     */
    NSPRFTD03,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, equipid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFListIDsForLoc}
     */
    NSPRFTD04,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, order1, order2,
     * order3, order4, order5, order6, order7, order8, order9, order10, order11, order12, order13, order14, order15,
     * order16, order17, order18, order19, order20, area01, area02, area03, area04, area05
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFTMDynamicPick}
     */
    NSPRFTDP1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, demandkey,
     * orderkey, orderlinenumber, storerkey, sku, loc, lpn, id1, qty1, lpn2, id2, qty2, lpn3, id3, qty3, toid, stage,
     * door, locid, packkey, uom, descr, taskdetailkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFValidateIds}
     */
    NSPRFTDP3,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * storerkey, sku, lot, loc, id, qty, yesorno
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.QuantityConfirmP1S1}
     */
    NSPRFTDPQCFN,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * storerkey, sku, lot, loc, id, qty, yesorno
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.QuantityConfirmP1S1}
     */
    NSPRFTDPQCFY,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, msgforerror
     * {@link com.ssaglobal.scm.wms.service.dcustomize.ThrowErrorFromRF}
     */
    NSPRFTHROWERROR,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, TTM, area01,
     * area02, area03, area04, area05, lastloc, lasttask, taskoverride, taskdetailkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTM01P1S1}
     */
    NSPRFTM01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, TTM,
     * taskdetailkey, reason {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTM03P1S1}
     */
    NSPRFTM03,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDirectedLoadUCP1S1}
     */
    NSPRFTM10,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, route, stop, date
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFCheckForLoadInformation}
     */
    NSPRFTM30,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttM, area,
     * sequence, continue, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14,
     * arg15, arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29,
     * arg30, ioflag, taskkey {@link com.ssaglobal.scm.wms.service.drfmanagement.TMEVCP02P1S1}
     */
    NSPRFTM310,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttM, area,
     * sequence, continue, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14,
     * arg15, arg16, arg17, arg18, arg19, arg20, arg21, arg22, arg23, arg24, arg25, arg26, arg27, arg28, arg29,
     * arg30, ioflag {@link com.ssaglobal.scm.wms.service.drfmanagement.TMEVCP01P1S1}
     */
    NSPRFTM410,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttM, area,
     * sequence, continue, wavekey, ioflag {@link com.ssaglobal.scm.wms.service.drfmanagement.TMEVCP03P1S1}
     */
    NSPRFTM510,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttM, area,
     * sequence, continue, loadid, ioflag, externalloadid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.TMEVCP04P1S1}
     */
    NSPRFTM610,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, TTM, area01,
     * area02, area03, area04, area05, lastloc, lasttask, taskoverride
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTMOMP1S1}
     */
    NSPRFTMOP01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, scrtype, rectype
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMPcc03P1S1}
     */
    NSPRFTMP00,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, scrtype, TTM,
     * taskdetailkey {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMPcc03P1S1}
     */
    NSPRFTMP01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey, loc,
     * storerkey, sku {@link com.ssaglobal.scm.wms.service.drfmanagement.TMEVPCCP1S1}
     */
    NSPRFTMPC,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey, loc,
     * sku {@link com.ssaglobal.scm.wms.service.drfmanagement.TMEVPCCP1S1}
     */
    NSPRFTMPCS,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, area, sequence,
     * continue {@link com.ssaglobal.scm.wms.service.drfmanagement.TMEVPIP1S1}
     */
    NSPRFTMPH01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * storerkey, lot, sku, id, loc, chkdigit, qty, uom, packkey, inventorytag, team
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.TMPHY03P1S1}
     */
    NSPRFTMPH03,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * loc {@link com.ssaglobal.scm.wms.service.drfmanagement.TMPHY04P1S1}
     */
    NSPRFTMPH04,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * storerkey, sku, lot, id, loc, chkdigit, qty, team, packkey, uom
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.TMPHY05P1S1}
     */
    NSPRFTMPH05,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * taskdetailkey, storerkey, sku, fromloc, fromchkdigit, fromid, toloc, tochkdigit, toid, lot, qty, packkey, uom,
     * reason, transactionkey {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTMV01P1S1}
     */
    NSPRFTMV01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * taskdetailkey, fromloc, fromid, reason {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTMV02P1S1}
     */
    NSPRFTMV02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * toloc, reason, toLocCheckDigit {@link com.ssaglobal.scm.wms.service.drfmanagement.TMMoveToFinalP1S1}
     */
    NSPRFTMV03,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.TMMoveEvaluteQtyP1S1}
     */
    NSPRFTMV2A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.TMUpdateMoveQtyP1S1}
     */
    NSPRFTMV2B,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.TMCancelMoveP1S1}
     */
    NSPRFTMVCANCEL,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, reasoncode,
     * fromto {@link com.ssaglobal.scm.wms.service.dtaskmanagement.RFValidTMReasonP1S1}
     */
    NSPRFTMVR01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * taskdetailkey, fromloc, fromid1, fromid2, fromid3, fromid4, fromid5, MultiPalletRemaining, area01, area02,
     * area03, area04, area05 {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMTPA00P1S1}
     */
    NSPRFTPA00,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * taskdetailkey, fromloc, fromid {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMTPA01P1S1}
     */
    NSPRFTPA01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * taskdetailkey, fromloc, fromid_lbl, fromid, toloc, tochkdigit, toid, qty, packkey, uom, reason
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMTPA02P1S1}
     */
    NSPRFTPA02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMTPA03AP1S1}
     */
    NSPRFTPA03A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm, fromloc,
     * fromid1, fromid2, fromid3, fromid4, fromid5, area01, area02, area03, area04, area05
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMTPAM0P1S1}
     */
    NSPRFTPAM0,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * taskdetailkey1, fromloc1, fromid1, toloc1, toid1, standard, qty1, packkey1, uom1, taskdetailkey2, fromloc2,
     * fromid2, toloc2, toid2, standard2, qty2, packkey2, uom2, taskdetailkey3, fromloc3, fromid3, toloc3, toid3,
     * standard3, qty3, packkey3, uom3, taskdetailkey4, fromloc4, fromid4, toloc4, toid4, standard4, qty4, packkey4,
     * uom4, taskdetailkey5, fromloc5, fromid5, toloc5, toid5, standard5, qty5, packkey5, uom5, confirmid, confirmloc
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMTPAM1P1S1}
     */
    NSPRFTPAM1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * taskdetailkey, fromloc, fromid, toloc, tochkdigit, toid1_lbl, toid, qty, packkey, uom, reason
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMTPA02P1S1}
     */
    NSPRFTPAM2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * taskdetailkey1, fromloc1, fromid1, toloc1, toid1, standard, qty1, packkey1, uom1, taskdetailkey2, fromloc2,
     * fromid2, toloc2, toid2, standard2, qty2, packkey2, uom2, taskdetailkey3, fromloc3, fromid3, toloc3, toid3,
     * standard3, qty3, packkey3, uom3, taskdetailkey4, fromloc4, fromid4, toloc4, toid4, standard4, qty4, packkey4,
     * uom4, taskdetailkey5, fromloc5, fromid5, toloc5, toid5, standard5, qty5, packkey5, uom5
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMTPAM3P1S1}
     */
    NSPRFTPAM3,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMPcc0P1S1}
     */
    nspRFTPC0,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, orderkey, loc,
     * storerkey,  sku {@link com.ssaglobal.scm.wms.service.dcustomize.RFGetPCTasksP1S1}
     */
    NSPRFTPC1A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sku, orderkey,
     * loc {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTPCR1P1S1}
     */
    NSPRFTPCR1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sku, orderkey,
     * loc {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTPCR1P1S2}
     */
    NSPRFTPCR2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * taskdetailkey, storerkey, sku, fromloc, fromchkdigit, fromid, toloc, tochkdigit, toid, lot, qty, caseid,
     * packkey, uom, reason, cartongroup, cartontype {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMTPKP1S1}
     */
    NSPRFTPK01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * taskdetailkey, storerkey, sku, fromloc, fromchkdigit, fromid, toloc, tochkdigit, toid, lot, qty, caseid,
     * packkey, uom, reason, cartongroup, cartontype, altsku
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMTPKP1S1}
     */
    NSPRFTPK01A,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * taskdetailkey, storerkey, sku, fromloc, fromchkdigit, fromid, toloc, tochkdigit, toid, lot, qty, caseid,
     * packkey, uom, reason, cartongroup, cartontype, clustertype, position
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMTPKP1S2}
     */
    NSPRFTPK01B,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * taskdetailkey, storerkey, sku, fromloc, fromchkdigit, fromid, toloc, tochkdigit, toid, lot, qty, caseid,
     * packkey, uom, reason, cartongroup, cartontype, transactionkey, position
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMTPKP1S3}
     */
    NSPRFTPK01C,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, caseid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.PrintAddLabelReportForCPP1S1}
     */
    NSPRFTPKPL,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * storerkey, sku, lot, loc, id, qty, yesorno
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.QuantityConfirmP1S1}
     */
    NSPRFTPKQCFN,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * storerkey, sku, lot, loc, id, qty, yesorno
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.QuantityConfirmP1S1}
     */
    NSPRFTPKQCFY,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, order1, order9,
     * tpk_reason, order11, order12, transactionkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFReasonCodeRequiredP1S1}
     */
    NSPRFTPKR01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * replenishmentgroup, taskdetailkey, storerkey, sku, fromloc, fromchkdigit, fromid, toloc, tochkdigit, toid,
     * lot, qty, packkey, uom, reason, itrnkey, picktaskdetailkey
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMTRP01P1S1}
     */
    NSPRFTPKRP01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, assignment,
     * taskdetailkey, storerkey, sku, fromloc, fromid, toloc, toid, lot, qty, caseid, packkey, uom, message01,
     * message02, message03, descr, fromchkdigit, tochkdigit, posvermethod1, posvermethod2, taskkey, cartongroup,
     * cartontype {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMTPKRP02P1S1}
     */
    NSPRFTPKRP02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey1,
     * taskdetailkey2 {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMTPKRP03P1S1}
     */
    NSPRFTPKRP03,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * replenishmentgroup, taskdetailkey, storerkey, sku, fromloc, fromchkdigit, fromid, toloc, tochkdigit, toid,
     * lot, qty, packkey, uom, reason, itrnkey, picktaskdetailkey, assignmentkey
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMTRP01P1S1}
     */
    NSPRFTPKRP04,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * replenishmentgroup, taskdetailkey, storerkey, sku, fromloc, fromchkdigit, fromid, toloc, tochkdigit, toid,
     * lot, qty, packkey, uom, reason, itrnkey, picktaskdetailkey, clusterkey, firstclustertask, lastclustertask,
     * closetote {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMTRP01P1S1}
     */
    NSPRFTPKRP05,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * replenishmentgroup, taskdetailkey, storerkey, sku, fromloc, fromchkdigit, fromid, toloc, tochkdigit, toid,
     * lot, qty, packkey, uom, reason, itrnkey, picktaskdetailkey, clusterkey, firstclustertask, lastclustertask,
     * closetote {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMTRP01P1S1}
     */
    NSPRFTPKRP06,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * replenishmentgroup, taskdetailkey, storerkey, sku, fromloc, fromchkdigit, fromid, toloc, tochkdigit, toid,
     * lot, qty, packkey, uom, reason, itrnkey, picktaskdetailkey, clusterkey, firstclustertask, lastclustertask,
     * closetote {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMTRP01P1S1}
     */
    NSPRFTPKRP07,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * replenishmentgroup, taskdetailkey, storerkey, sku, fromloc, fromchkdigit, fromid, toloc, tochkdigit, toid,
     * lot, qty, packkey, uom, reason, itrnkey, picktaskdetailkey, clusterkey, firstclustertask, lastclustertask,
     * closetote {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMTRP01P1S1}
     */
    NSPRFTPKRP08,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, trailer, trltype,
     * door, carrier, asn1, asn2, asn3, asn4, asn5 {@link com.ssaglobal.scm.wms.service.drfmanagement.AddTrailer}
     */
    NSPRFTR1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, trailer,
     * trailerkey {@link com.ssaglobal.scm.wms.service.drfmanagement.SearchTrailer}
     */
    NSPRFTR2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, trailer, trltype,
     * door, carrier, trlstatus, temp1, temp2, temp3, seal1, seal2, seal3, seal4, seal5, seal1status, cleanstatus,
     * notes, trailerkey {@link com.ssaglobal.scm.wms.service.drfmanagement.UpdateTrailer}
     */
    NSPRFTR3,
    /**
     * {@link com.ssaglobal.scm.wms.service.dcustomize.TransferFinalizer}
     */
    NSPRFTRANSFERFINALIZER,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, trailer
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.TrailerKeyLookup}
     */
    NSPRFTRLKEYLKUP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, replgroup,
     * taskkey, stor, prod, desc, from, id, to, id, lot, qty, pack, uom
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFTRPP1S1}
     */
    NSPRFTRP,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, ttm,
     * replenishmentgroup, taskdetailkey, storerkey, sku, fromloc, fromchkdigit, fromid, toloc, tochkdigit, toid,
     * lot, qty, packkey, uom, reason, itrnkey, transactionkey
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMTRP01P1S1}
     */
    NSPRFTRP01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * fromloc, fromid {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTSP01P1S1}
     */
    NSPRFTSP01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * fromloc, fromid, tag, storerkey, sku, lot, toloc, qty
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTSP02P1S1}
     */
    NSPRFTSP02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * fromloc, fromid, tag, storerkey, sku, lot, toloc, tochkdigit, qty, packkey, uom, reason
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.TMTSP03P1S1}
     */
    NSPRFTSP03,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFUnLoadLPN0P1S1}
     */
    nspRFUL0,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, trailerid, lpn,
     * toloc {@link com.ssaglobal.scm.wms.service.drfmanagement.RFUnloadLPNP1S1}
     */
    nspRFUL01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, value1, value2
     * {@link com.ssaglobal.scm.wms.service.dcustomize.ValuesComparator}
     */
    NSPRFVC,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, asn
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.ValidateASN}
     */
    NSPRFVCQ01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, seq, question,
     * reply, format, anscode, startctr, endctr, shipfrom, asn, stgykey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.VendorQuestions}
     */
    NSPRFVCQ02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, pro
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.VendorMessage}
     */
    NSPRFVDMSG,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, containerdetailid
     * {@link com.ssaglobal.scm.wms.service.dlabelprinting.VoidContainerLabels1P1S1}
     */
    NSPRFVOUTLBL1,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server,
     * containerdetailid, childlabels, voidchildflag
     * {@link com.ssaglobal.scm.wms.service.dlabelprinting.VoidContainerLabels2P1S1}
     */
    NSPRFVOUTLBL2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, POSID
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFTransshipVerP1S1}
     */
    NSPRFVT01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, DocumentKey,
     * ContainerID, SKU, UDF2, Qty, Damage, UDF1, CustomerKey, Vendor, Memo, PrinterID, CurrentLoc, Refsid, RetFlag,
     * transasnkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFTransshipASNReceiptP1S1}
     */
    NSPRFVT02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, DocumentKey,
     * ContainerID, SKU, UDF2, Qty, Damage, UDF1, CustomerKey, Vendor, Memo, PrinterID, CurrentLoc, Refsid, RetFlag,
     * transasnkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFTransshipASNReceiptP1S1}
     */
    NSPRFVT03,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, DocumentKey,
     * ContainerID, Cube, Weight, PrinterID, transasnkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFTransshipUpdateWCP1S1}
     */
    NSPRFVT04,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, workorderid,
     * operationstep, status, qtycomplete, id {@link com.ssaglobal.scm.wms.service.drfmanagement.RFWC01P1S1}
     */
    NSPRFWC01,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, workorderid,
     * dquantity, action {@link com.ssaglobal.scm.wms.service.drfmanagement.RFWC02P1S1}
     */
    NSPRFWC02,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, moveId
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFWC03P1S1}
     */
    NSPRFWC03,
    /**
     * Parameters: ssendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * woid, cops, cloc, cid, tops, tloc, tid, qty {@link com.ssaglobal.scm.wms.service.drfmanagement.RFWC031P1S1}
     */
    NSPRFWC030,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey,
     * woid, cops, cloc, cid, tops, tloc, tid, qty
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFWC031P1S1}
     */
    NSPRFWC031,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, taskdetailkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFWC032P1S1}
     */
    NSPRFWC032,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, Id, Action
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFWC04P1S1}
     */
    NSPRFWC04,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, workorderid,
     * operatorid, action {@link com.ssaglobal.scm.wms.service.drfmanagement.RFWC05P1S1}
     */
    NSPRFWC05,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, inboundjobid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFWC10P1S1}
     */
    NSPRFWC10,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, inboundjobid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFWC11P1S1}
     */
    NSPRFWC11,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, inboundjobid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFWC12P1S1}
     */
    NSPRFWC12,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, locale, password,
     * component, tenant, token, instance
     * {@link com.ssaglobal.scm.wms.service.webservices.framework.MetaRFDataBasesP1S2}
     */
    NSPRFWEBRFOT08,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, gwt1, gwt, nwt1,
     * nwt, tare1, tare, counter, visgwt, visnwt, vistare, totlinenumber
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFWeightCaptureAutoCompute}
     */
    NSPRFWGTSCAN2,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, storerkey, sku,
     * sourcekey, sourcelinenumber, lot, loc, fromloc, toloc, id, fromid, caseid, dropid, uom1, packkey1, uom2,
     * packkey2, qty, pickdetailkey, other2, other3, other4, other5, printerid, totalwgt, data1, wgt1, data2, wgt2,
     * data3, wgt3, data4, wgt4, data5, wgt5, data6, wgt6, data7, wgt7, data8, wgt8, data9, wgt9, data10, wgt10,
     * processtype, trantype, itrnkey, lot, action, visgwt, gwt, visnwt, nwt, vistare, tare, order9, TYP,
     * transactionkey {@link com.ssaglobal.scm.wms.service.drfmanagement.CatchWeightTrackingP1S1}
     */
    NSPRFWM01,
    /**
     * Parameters: dropid {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TMShipDropIdP1S1}
     */
    NSPSHIPDROPID,
    /**
     * Parameters: wavekey, IgnoreIncompleteTasks, IgnoreUnsortedBatchPicks
     * {@link com.ssaglobal.scm.wms.service.dordermanagement.ShipWave}
     */
    NSPSHIPWAVE,
    /**
     * Parameters: storerkey, sku {@link com.ssaglobal.scm.wms.service.dutilitymanagement.SkuDescriptionTranslationP1S1}
     */
    NSPSKUDESCTRANS,
    /**
     * Parameters: storerkey, sku, loc, sltgrp {@link com.ssaglobal.scm.wms.service.dutilitymanagement.Slotting}
     */
    NSPSLOTTING,
    /**
     * Parameters: StorerKeyMin, StorerKeyMax, SkuMin, SkuMax, DateMin, DateMax, locale
     * {@link com.ssaglobal.scm.wms.service.dreportmanagement.StockHiLoSkuP1S1}
     */
    NSPSTOCKHILOSKU,
    /**
     * Parameters: StorerKeyMin, StorerKeyMax, SkuMin, SkuMax, LotMin, LotMax, DateMin, DateMax
     * {@link com.ssaglobal.scm.wms.service.dreportmanagement.StockMovementLotP1S1}
     */
    NSPSTOCKMOVEMENTLOT,
    /**
     * Parameters: StorerKeyMin, StorerKeyMax, SkuMin, SkuMax, LotMin, LotMax, DateMin, DateMax
     * {@link com.ssaglobal.scm.wms.service.dreportmanagement.StockMovementLotDetailP1S1}
     */
    NSPSTOCKMOVEMENTLOTDETAIL,
    /**
     * Parameters: StorerKeyMin, StorerKeyMax, SkuMin, SkuMax, LotMin, LotMax, DateMin, DateMax
     * {@link com.ssaglobal.scm.wms.service.dreportmanagement.StockMovementSkuP1S1}
     */
    NSPSTOCKMOVEMENTSKU,
    /**
     * Parameters: StorerKeyMin, StorerKeyMax, SkuMin, SkuMax, LotMin, LotMax, DateMin, DateMax
     * {@link com.ssaglobal.scm.wms.service.dreportmanagement.StockMovementSkuDetailP1S1}
     */
    NSPSTOCKMOVEMENTSKUDETAIL,
    /**
     * Parameters: activity, userid, userattendancekey, starttime, duration, actype, paid
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.SupervisorIndirectActivityP1S1}
     */
    NSPSUPERVISORINDIRECTACTIVITY,
    /**
     * Parameters: type, action, data {@link com.ssaglobal.scm.wms.service.dtaskmanagement.Timesheet}
     */
    NSPTIMESHEET,
    /**
     * Parameters: Trailerkey, Trailer, Carrier {@link com.ssaglobal.scm.wms.service.drfmanagement.CreateNewTrailer}
     */
    nspTrailerNewInstance,
    /**
     * Parameters: userid, useractivitykey, assignmentnumber
     * {@link com.ssaglobal.scm.wms.service.dtaskmanagement.TransferPicksP1S1}
     */
    NSPTRANSFERPICKS,
    /**
     * Parameters: ORDERKEY, ORDERLINENUMBER {@link com.ssaglobal.scm.wms.service.dordermanagement.UnAllocOrders}
     */
    NSPUNALLOCATEORDERS,
    /**
     * Parameters: wavekey {@link com.ssaglobal.scm.wms.service.dordermanagement.UnAllocateWave}
     */
    NSPUNALLOCATEWAVE,
    /**
     * Parameters: assignment, userid {@link com.ssaglobal.scm.wms.service.dtaskmanagement.UnassignedWorkP1S1}
     */
    NSPUNASSIGNEDWORK,
    /**
     * Parameters: wavekey {@link com.ssaglobal.scm.wms.service.dordermanagement.UnconsolidateP1S1}
     */
    NSPUNCONSOLIDATE,
    /**
     * Parameters: storerkey, sku, loc, reltype {@link com.ssaglobal.scm.wms.service.dutilitymanagement.Unslotting}
     */
    NSPUNSLOTTING,
    /**
     * Parameters: fromqty,  fromuom,  touom,  packkey
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.UOMConversionP1S1}
     */
    NSPUOMCONV,
    /**
     * Parameters: AREAKEY, ALLOWVOICE, AUTOASSIGN, MAXNUMWORKIDS, SKIPAISLEALLOWED, SKIPLOCALLOWED, REPICKSKIPS,
     * SPEAKDESCRIPTION, PICKPROMPT, SPEAKWORKID, SIGNOFFALLOWED, DELIVERY, QUANTITYVERIFICATION, WORKIDLENGTH,
     * NOCONTAINERTRACKING, CURRENTAISLE, CURRENTSLOT, VERIFYPUTQTY, CONTAINERMETHOD, MULTIPLECONTAINERSALLOWED,
     * SPOKENCONTAINERLENGTH, SUMMARYPROMPTTYPE, SPEAKLEADINGZEROS, AISLESTART, AISLEEND, BAYSTART, BAYEND,
     * SLOTSTART, SLOTEND, NOTIFYOPERATOR, DROPIDREQUIRED, VALIDATECASEID, VALIDATEDROPID
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.UpdateAreaP1S1}
     */
    NSPUPDATEAREA,
    /**
     * Parameters: RECEIPTKEY {@link com.ssaglobal.scm.wms.service.dutilitymanagement.UpdateASNStatusP1S1}
     */
    NSPUPDATEASNSTATUS,
    /**
     * Parameters: Receiptkey, ReceiptLineNumber, SupplierName, Address1, Address2, City, State, Zip, Country, Phone,
     * ID {@link com.ssaglobal.scm.wms.service.dutilitymanagement.UpdateSupplierInfo}
     */
    NSPUPDATESUPPLIERINFO,
    /**
     * Parameters: userid {@link com.ssaglobal.scm.wms.service.dtaskmanagement.UserExtendedPeriodP1S1}
     */
    NSPUSEREXTENDEDPERIOD,
    /**
     * Parameters: GroupID {@link com.ssaglobal.scm.wms.service.dworkcenter.WCOrderProcessP1S1}
     */
    NSPWCOrderProcess,
    /**
     * Parameters: workorderid {@link com.ssaglobal.scm.wms.service.dworkcenter.WOPreallocate}
     */
    NSPWOPREALLOCATE,
    /**
     * Parameters: BatchID {@link com.ssaglobal.scm.wms.service.dordermanagement.WPRelease}
     */
    NSPWPRelease,
    /**
     * Parameters: userRoleID, eventType {@link com.ssaglobal.scm.wms.service.dutilitymanagement.AddRoleEventP1S1}
     */
    OAROLEADDEVENT,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.OutboundIDAPI}
     */
    OutboundIDAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.OwnerAPI}
     */
    OwnerAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.OwnerBillToAPI}
     */
    OwnerBillToAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.PackAPI}
     */
    PackAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.PickDetailAPI}
     */
    PickDetailAPI,
    /**
     * Parameters: receiptkey, receiptlinenumber, labelname, printername, copies
     * {@link com.ssaglobal.scm.wms.service.dlabelprinting.PrintASNLabelP1S1}
     */
    PrintASNLables,
    /**
     * Parameters: receiptkey, printername, labelname, copies
     * {@link com.ssaglobal.scm.wms.service.dlabelprinting.PrintPPLabelP1S1}
     */
    PrintPPLables,
    /**
     * Parameters: storerkey, shiptocode, carriercode, labelcount, printername
     * {@link com.ssaglobal.scm.wms.service.dlabelprinting.PrintRFIDDropIDP1S1}
     */
    PRINTRFIDDROPIDP1S1,
    /**
     * Parameters: StorerKey, shiptocode, carriercode, Sku, printername
     * {@link com.ssaglobal.scm.wms.service.dlabelprinting.PrintRFIDGTINP1S1}
     */
    PRINTRFIDGTINP1S1,
    /**
     * Parameters: printername {@link com.ssaglobal.scm.wms.service.dlabelprinting.PrintTestLWS}
     */
    PrintTestLWST,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.PurchaseOrderAPI}
     */
    PurchaseOrderAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.PutawayStrategyAPI}
     */
    PutawayStrategyAPI,
    /**
     * Parameters: putawaystrategy, storerkey, sku, id, lot, packkey, fromloc, qty, ptrace
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.PutawayTraceP1S1}
     */
    PUTAWAYTRACE,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.PutawayZoneAPI}
     */
    PutawayZoneAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.ReceiptAPI}
     */
    ReceiptAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.ReceivedInventoryAPI}
     */
    ReceivedInventoryAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.ReportAPI}
     */
    ReportAPI,
    /**
     * Parameters: CLIENTTABLE, INSTANCE, USERLIST {@link com.ssaglobal.scm.wms.service.drfmanagement.RFClientTableGC}
     */
    RFCLIENTTABLEGC,
    /**
     * Parameters: callerID, instance, component, sendDelimiter, userid, password
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFDataBaseP1S1}
     */
    RFDataBaseP1S1,
    /**
     * Parameters:  {@link com.ssaglobal.scm.wms.service.dcrossdock.RFLoadManagement}
     */
    RFLOADMANAGEMENT,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.SectionAPI}
     */
    SectionAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.SecurityRoleAPI}
     */
    SecurityRoleAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.SecurityRolesFacilityAPI}
     */
    SecurityRolesFacilityAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.SecurityRolesReportAPI}
     */
    SecurityRolesReportAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.SecurityRolesScreensAPI}
     */
    SecurityRolesScreensAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.SecurityUserAPI}
     */
    SecurityUserAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.SecurityUserPartnerAPI}
     */
    SecurityUserPartnerAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.SecurityUserRolesAPI}
     */
    SecurityUserRolesAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.SelectionZoneAPI}
     */
    SelectionZoneAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.SerialNumberAPI}
     */
    SerialNumberAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.SharedProdLocAPI}
     */
    SharedProdLocAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.ShipFromAPI}
     */
    ShipFromAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.ShipmentOrderAPI}
     */
    ShipmentOrderAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.ShipToAPI}
     */
    ShipToAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.SortationStationAPI}
     */
    SortationStationAPI,
    /**
     * Parameters: pickdetailkey, dropid, cartontype, qty
     * {@link com.ssaglobal.scm.wms.service.spsintegration.SplitCaseID}
     */
    SplitCaseID,
    /**
     * Parameters: pickdetailkey, lpn, qty {@link com.ssaglobal.scm.wms.service.drfmanagement.SplitPickDetailHelper}
     */
    SplitPickDetail,
    /**
     * Parameters: ids {@link com.ssaglobal.scm.wms.service.spsintegration.inforsps.SPSDeleteInternational}
     */
    SPSDeleteInternational,
    /**
     * {@link com.ssaglobal.scm.wms.service.spsintegration.inforsps.SPSInternationalData}
     */
    SPSInternationalData,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.StorerAPI}
     */
    StorerAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.SubstituteSKUAPI}
     */
    SubstituteSKUAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.SupplierAPI}
     */
    SupplierAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.TaskAPI}
     */
    TaskAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.TaskManagerUserAPI}
     */
    TaskManagerUserAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.TaskReasonAPI}
     */
    TaskReasonAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.TransferAPI}
     */
    TransferAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.TransShipASNAPI}
     */
    TransShipASNAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.TransShipOrderAPI}
     */
    TransShipOrderAPI,
    /**
     * Parameters: toid, orderkey, fromid, sku, lot, qty,  transactionkey,  pickdetailkey
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackingP4S4}
     */
    UIPACKING01,
    /**
     * Parameters: orderkey, fromid {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackFromIDCompleteP1S1}
     */
    UIPACKINGCompleteFromID,
    /**
     * Parameters: toid, orderkey {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackToIDCompleteP1S1}
     */
    UIPACKINGCompleteToID01,
    /**
     * Parameters: toid, orderkey, carrier, stage, door, trackingid, weight, droploc, pckprinterid1, pckcopies1,
     * pckprinterid2, pckcopies2, pckprinterid3, pckcopies3, pckprinterid4, pckcopies4, pckprinterid5, pckcopies5,
     * pckprinterid6, pckcopies6, fromScreen
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackToIDCompleteP2S2}
     */
    UIPACKINGCompleteToID02,
    /**
     * Parameters: fromid, fromloc, toid, toloc, action
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.ServiceSHP1S1}
     */
    UIPACKINGPACKFULLLPN,
    /**
     * Parameters: IDType,  ID,  PrintAddLbl,  LblPrinter,  LabelCopies,  PrintCompliantLbl,  CLblPrinter,
     * Clabelcopies,  PrintContentRpt,  RptPrinter,  Rptcopies
     * {@link com.ssaglobal.scm.wms.service.dlabelprinting.PrintAddrLabelReportP1S1}
     */
    UIPACKINGPrint,
    /**
     * Parameters: fromid, action {@link com.ssaglobal.scm.wms.service.drfmanagement.ServiceSHP1S1}
     */
    UIPACKINGShipID,
    /**
     * Parameters: toid, orderkey, fromid, sku, storerkey, serialnumber, lot, pickdetailkey
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.SerialNumberScanPackP1S1}
     */
    UISCAN_PACK,
    /**
     * Parameters: toid {@link com.ssaglobal.scm.wms.service.drfmanagement.RFQCPackUnPackToIDP1S1}
     */
    UIUNPACKING01,
    /**
     * Parameters: Wavekey {@link com.ssaglobal.scm.wms.service.dordermanagement.UnBatchOrderP1S1}
     */
    UNBATCHORDER,
    /**
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.UpdateCheckDigitToExistingLocs}
     */
    UPDATECHECKDIGIT,
    /**
     * Parameters: orderkey, door, stage, packingloc, priority
     * {@link com.ssaglobal.scm.wms.service.dordermanagement.UpdateDockAssignmentforOrder}
     */
    UpdateDockAssignmentforOrder,
    /**
     * Parameters: Userid {@link com.ssaglobal.scm.wms.service.dmultifacility.UpdateEnterpriseData}
     */
    UPDATEENTERPRISEDATA,
    /**
     * {@link com.ssaglobal.scm.wms.service.dmultifacility.UpdateFacilityNest}
     */
    UPDATEFACILITYNEST,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.UserDataTranslationAPI}
     */
    UserDataTranslationAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.UtilityAPI}
     */
    UtilityAPI,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sortstation,
     * wavekey, orderkey {@link com.ssaglobal.scm.wms.service.drfmanagement.ValidateBatchFields}
     */
    VALIDATEBATCHFIELDS,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sortstation,
     * wavekey, orderkey, available, sorted, total {@link com.ssaglobal.scm.wms.service.drfmanagement.ValidateShortPick}
     */
    VALIDATESHORTPICK,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sortstation,
     * wavekey, orderkey, available, sorted, total
     * {@link com.ssaglobal.scm.wms.service.dcustomize.sorting.ValidateShortPickLT}
     */
    VALIDATESHORTPICKLT,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, sortstation
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.ValidateSortStn}
     */
    VALIDATESORTSTATION,
    /**
     * Parameters: sendDelimiter, ptcid, userid, taskId, databasename, appflag, recordType, server, toid
     * {@link com.ssaglobal.scm.wms.service.drfmanagement.ValidateToID}
     */
    VALIDATETOID,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.VoiceAPI}
     */
    VoiceAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.VoiceTaskManagerAPI}
     */
    VoiceTaskManagerAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.WaveAPI}
     */
    WaveAPI,
    /**
     * Parameters: wavekey, printerid, copies {@link com.ssaglobal.scm.wms.service.spsintegration.WaveSPSLabel}
     */
    WaveSPSLabel,
    /**
     * Parameters: methodCalled, json {@link com.ssaglobal.scm.wms.service.webservices.WebServiceController}
     */
    WebServiceController,
    /**
     * Parameters: wgtadjustmentkey, weightadjustmentlinenumber, storerkey, sku, ADJGROSSWGT, ADJNETWGT, ADJTAREWGT,
     * GROSSWGT, NETWGT, TAREWGT, LOT, LOC, ID, qty, trantype
     * {@link com.ssaglobal.scm.wms.service.dwarehousemanagement.WeightsAdjustment}
     */
    WeightsAdjustment,
    /**
     * Parameters: userQry, maxOrders, maxOrderLines, maxCube, maxWeight, maxCases, maxRoutes
     * {@link com.ssaglobal.scm.wms.service.dutilitymanagement.QueryBuilderShowOrderListP1S1}
     */
    WPSHOWORDERLIST,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.ZoneAPI}
     */
    ZoneAPI,
    /**
     * Parameters: user, function, xml {@link com.ssaglobal.scm.wms.service.dexternalapis.ZoneLaborAPI}
     */
    ZoneLaborAPI,
}
