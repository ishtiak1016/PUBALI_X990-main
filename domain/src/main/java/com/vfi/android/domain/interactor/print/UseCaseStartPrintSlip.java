package com.vfi.android.domain.interactor.print;

import static com.vfi.android.domain.entities.databeans.PrinterParamIn.ALIGN_LEFT;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vfi.android.domain.R;
import com.vfi.android.domain.entities.businessbeans.CurrentTranData;
import com.vfi.android.domain.entities.businessbeans.HostInfo;
import com.vfi.android.domain.entities.businessbeans.MerchantInfo;
import com.vfi.android.domain.entities.businessbeans.PrintConfig;
import com.vfi.android.domain.entities.businessbeans.RecordInfo;
import com.vfi.android.domain.entities.businessbeans.TerminalCfg;
import com.vfi.android.domain.entities.businessbeans.TerminalStatus;
import com.vfi.android.domain.entities.consts.CVMResult;
import com.vfi.android.domain.entities.consts.CardEntryMode;
import com.vfi.android.domain.entities.consts.CardType;
import com.vfi.android.domain.entities.consts.HostType;
import com.vfi.android.domain.entities.consts.PrintType;
import com.vfi.android.domain.entities.consts.TAGS;
import com.vfi.android.domain.entities.consts.TransType;
import com.vfi.android.domain.entities.databeans.PrintInfo;
import com.vfi.android.domain.entities.databeans.PrintTask;
import com.vfi.android.domain.entities.databeans.PrinterParamIn;
import com.vfi.android.domain.entities.databeans.SettlePrintItem;
import com.vfi.android.domain.entities.databeans.SettlementRecord;
import com.vfi.android.domain.entities.databeans.SettlementRecordItem;
import com.vfi.android.domain.executor.PostExecutionThread;
import com.vfi.android.domain.executor.ThreadExecutor;
import com.vfi.android.domain.interactor.UseCase;

import com.vfi.android.domain.interactor.print.tasks.InstallmentPrintTask;
import com.vfi.android.domain.interactor.print.tasks.CommonPrintTask;
import com.vfi.android.domain.interactor.print.tasks.LogonPrintTask;
import com.vfi.android.domain.interactor.print.tasks.VoidPrintTask;
import com.vfi.android.domain.interactor.transaction.UseCaseStartTransCommunication;
import com.vfi.android.domain.interfaces.repository.IRepository;
import com.vfi.android.domain.utils.LogUtil;
import com.vfi.android.domain.utils.StringUtil;
import com.vfi.android.domain.utils.Tlv2Map;
import com.vfi.android.domain.utils.ZlibUtil;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;


public class UseCaseStartPrintSlip extends UseCase<Integer, PrintInfo> {
    private final String TAG = TAGS.PRINT;
    private final IRepository iRepository;
    private final PrintTaskManager printTaskManager;
    private CurrentTranData currentTranData;
    private RecordInfo recordInfo;

    @Inject
    UseCaseStartPrintSlip(PrintTaskManager printTaskManager,
                          IRepository iRepository,
                          ThreadExecutor threadExecutor,
                          PostExecutionThread postExecutionThread) {
        super(threadExecutor, postExecutionThread);
        this.iRepository = iRepository;
        this.printTaskManager = printTaskManager;
        this.currentTranData = iRepository.getCurrentTranData();
        this.recordInfo = currentTranData.getRecordInfo();
    }


    @Override
    public Observable<Integer> buildUseCaseObservable(PrintInfo printInfo) {
        LogUtil.d(TAG, "UseCaseStartPrintSlip executed.");
        this.currentTranData = iRepository.getCurrentTranData();
        this.recordInfo = currentTranData.getRecordInfo();
        return Observable.create(emitter -> {
            if (printInfo == null) {
                LogUtil.d(TAG, "No print info");
                emitter.onComplete();
            }


            printInfo.setiRepository(iRepository);

            if (printInfo.isContinueFromPrintError()) {
                LogUtil.d(TAG, "isContinueFromPrintError true");
                printTaskManager.continuePrint(new IPrintListener() {
                    @Override
                    public void onSuccess(int taskId) {
                        emitter.onNext(taskId);
                        emitter.onComplete();
                    }

                    @Override
                    public void onError(Throwable throwable) {


                        emitter.onError(throwable);
                    }
                });
                return;
            }

            // set reprint record.
            if (printInfo.isDuplicateSlip()) {
                recordInfo = printInfo.getRecordInfo();
                LogUtil.d(TAG, "recordInfo = " + recordInfo.toString());
            }

            if ( isEnableISOLog() && !printInfo.isDuplicateSlip() && !printInfo.isContinueFromPrintError()) {
                Log.d("ishtiak",""+printInfo.getPrintType());
               if(printInfo.getPrintType()==4||printInfo.getPrintType()==5||printInfo.getPrintType()==14){
               }else{
                   startPrintISOLog();
               }

            }

            if (printInfo.getPrintTask() != null) {
                LogUtil.d(TAG, "Print presentation print task.");
                startPrintTask(emitter, printInfo.getPrintTask());
                return;
            } else {
                LogUtil.d(TAG, "Print type[" + PrintType.toDebugString(printInfo.getPrintType()) + "] " + printInfo.getPrintType());
                switch (printInfo.getPrintType()) {
                    case PrintType.SETTLEMENT:
                        saveSettlementPrintData();
                        printSettlementSlip(emitter, printInfo);
                        break;
                    case PrintType.SUMMARY_REPORT:
                        printSummaryReportSlip(emitter, printInfo);
                        break;
                    case PrintType.DETAIL_REPORT:
                        printDetailReportSlip(emitter, printInfo);
                        break;
                    case PrintType.LAST_SETTLEMENT:
                        printLastSettlementData(emitter, printInfo);
                        break;
                    case PrintType.LAST_TRANS:
                        printLastTransData(emitter, printInfo);
                        break;
                    case PrintType.EMV_DEBUG_INFO:
                        printEmvDebugInfo(emitter, printInfo);
                        break;
                    case PrintType.ISO_LOG:
                        printLastISOLog(emitter, printInfo);
                        break;
                    default:
                        saveTransPrintData(printInfo);
                        printNormalSlip(emitter, printInfo);
                        break;
                }
            }
        });
    }

    private void saveSettlementPrintData() {
        SettlementRecord settlementRecord = currentTranData.getCurrentSettlementRecord();
        if (settlementRecord != null) {
            TerminalStatus terminalStatus = iRepository.getTerminalStatus();
            Gson gson = new GsonBuilder().create();
            String value = gson.toJson(settlementRecord);
            terminalStatus.setLastSettlementPrintData(value);
            iRepository.putTerminalStatus(terminalStatus);
        }
    }

    private void saveTransPrintData(PrintInfo printInfo) {
        if (!printInfo.isDuplicateSlip() && recordInfo != null) {
            TerminalStatus terminalStatus = iRepository.getTerminalStatus();
            Gson gson = new GsonBuilder().create();
            String value = gson.toJson(recordInfo);
            terminalStatus.setLastTransPrintData(value);
            iRepository.putTerminalStatus(terminalStatus);
        }
    }

    private void printLastTransData(ObservableEmitter emitter, PrintInfo printInfo) {
        TerminalStatus terminalStatus = iRepository.getTerminalStatus();
        Gson gson = new GsonBuilder().create();
        try {
            recordInfo = gson.fromJson(terminalStatus.getLastTransPrintData(), RecordInfo.class);
            currentTranData.setRecordInfo(recordInfo);
            printInfo.setDuplicateSlip(true);
            printNormalSlip(emitter, printInfo);
        } catch (Exception e) {
            e.printStackTrace();
            emitter.onNext(0);
            emitter.onComplete();
        }
    }

    private void printEmvDebugInfo(ObservableEmitter emitter, PrintInfo printInfo) {
        TerminalStatus terminalStatus = iRepository.getTerminalStatus();

        PrintTask printTask = new PrintTask();

        addLogoImage(printTask, printInfo);
        addOneBlackLine(printTask);
        printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, PrinterParamIn.ALIGN_CENTER, false, "Last Emv Debug Info");
        addOneDividingLine(printTask, '-');

        Map<String, String> map = Tlv2Map.tlv2Map(terminalStatus.getLastEmvDebugInfo());
        if (map.size() == 0) {
            printTask.addPrintLine(PrinterParamIn.FONT_SMALL, PrinterParamIn.ALIGN_CENTER, false, "- - - -  No Data - - - -");
        } else {
            Iterator<Map.Entry<String, String>> tlvIterator = map.entrySet().iterator();
            while (tlvIterator.hasNext()) {
                Map.Entry<String, String> entry = tlvIterator.next();
                String type = entry.getKey();
                String tlvValue = entry.getValue();
                printTask.addPrintLine(PrinterParamIn.FONT_SMALL, false, "   " + type, tlvValue);
            }
        }
        addOneDividingLine(printTask, '-');
        addOneBlackLine(printTask);
        addOneBlackLine(printTask);
        addOneBlackLine(printTask);

        printTaskManager.addPrintTask(printTask, new IPrintListener() {
            @Override
            public void onSuccess(int taskId) {
                emitter.onNext(taskId);
                emitter.onComplete();
            }

            @Override
            public void onError(Throwable throwable) {
                emitter.onError(throwable);
            }
        });
    }

    private void printLastSettlementData(ObservableEmitter emitter, PrintInfo printInfo) {
        TerminalStatus terminalStatus = iRepository.getTerminalStatus();
        Gson gson = new GsonBuilder().create();
        try {
            SettlementRecord settlementRecord = gson.fromJson(terminalStatus.getLastSettlementPrintData(), SettlementRecord.class);
            currentTranData.setCurrentSettlementRecord(settlementRecord);
            printInfo.setDuplicateSlip(true);
            // printInfo.setPrintLogoData(getl);
            printSettlementSlip(emitter, printInfo);
        } catch (Exception e) {
            e.printStackTrace();
            emitter.onNext(0);
            emitter.onComplete();
        }
    }

    private void printLastISOLog(ObservableEmitter emitter, PrintInfo printInfo) {
        TerminalStatus terminalStatus = iRepository.getTerminalStatus();

        String reqISOLog = terminalStatus.getReqIsoLog();
        String respISOLog = terminalStatus.getRespIsoLog();
        printISOLog("ISO Request Message", reqISOLog, null);
        printISOLog("ISO Response Message", respISOLog, new IPrintListener() {
            @Override
            public void onSuccess(int taskId) {
                emitter.onNext(-1);
                emitter.onComplete();
            }

            @Override
            public void onError(Throwable throwable) {
                emitter.onError(throwable);
            }
        });
    }

    private boolean isEnableISOLog() {
        TerminalCfg terminalCfg = iRepository.getTerminalCfg();
        LogUtil.d(TAG, "isEnableISOLog=" + terminalCfg.isEnableISOLog());
        return terminalCfg.isEnableISOLog();
    }

    private void startPrintISOLog() {
        TerminalStatus terminalStatus = iRepository.getTerminalStatus();
        String reqISOLog = terminalStatus.getReqIsoLog();
        String respISOLog = terminalStatus.getRespIsoLog();
        printISOLog("ISO Request Message", reqISOLog, null);
        printISOLog("ISO Response Message", respISOLog, null);
    }

    private void printISOLog(String header, String isoLog, IPrintListener printListener) {
        PrintTask printTask = new PrintTask();
        printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, PrinterParamIn.ALIGN_CENTER, false, header);
        addOneDividingLine(printTask, '-');

        if (isoLog == null || isoLog.length() == 0) {
            LogUtil.d(TAG, "ISO Log[" + header + "] empty.");
            printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, PrinterParamIn.ALIGN_CENTER, false, "- - - - Empty ISO Log - - - -");
            addOneDividingLine(printTask, '-');
            addOneBlackLine(printTask);
            if (printListener != null) {
                addOneBlackLine(printTask);
                addOneBlackLine(printTask);
            }
            printTaskManager.addPrintTask(printTask, printListener);
            return;
        }

        try {

            JSONObject isoLogJson = new JSONObject(isoLog);
            Iterator<String> iterator = isoLogJson.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                String value = isoLogJson.getString(key);
                if (key.equals("F55")) {
                    printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, false, key, value);
                    Map<String, String> map = Tlv2Map.tlv2Map(value);
                    Iterator<Map.Entry<String, String>> tlvIterator = map.entrySet().iterator();
                    while (tlvIterator.hasNext()) {
                        Map.Entry<String, String> entry = tlvIterator.next();
                        String type = entry.getKey();
                        String tlvValue = entry.getValue();
                        printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, false, "   " + type, tlvValue);
                    }
                } else {
                    printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, false, key, value);
                }
            }
            addOneBlackLine(printTask);
            addOneBlackLine(printTask);
        } catch (Exception e) {
            e.printStackTrace();
        }

        printTaskManager.addPrintTask(printTask, null);
    }

    private void printNormalSlip(ObservableEmitter emitter, PrintInfo printInfo) {
        LogUtil.d(TAG, "printNormalSlip excuted.");
        //Log.e("T_CHECK_001",(recordInfo != null)+" recordInfo");
        if (recordInfo != null) {
            printInfo.setPrintConfig(iRepository.getPrintConfig(recordInfo.getMerchantIndex()));
        }

        PrintTask printTask = getPrintTask(printInfo);

        if (printTask == null) {
            throw new RuntimeException("Create print task failed.");
        }
        Log.e("T_CHECK_001", (printTask != null) + " printerInfo");
        printTaskManager.addPrintTask(printTask, new IPrintListener() {
            @Override
            public void onSuccess(int taskId) {
                emitter.onNext(taskId);
                emitter.onComplete();

            }

            @Override
            public void onError(Throwable throwable) {
                emitter.onError(throwable);
                Log.e("T_CHECK_001", throwable.getLocalizedMessage() + " recordInfo");
            }
        });
    }

    private void printSettlementSlip(ObservableEmitter emitter, PrintInfo printInfo) {
        int taskId = 0;
        SettlementRecord settlementRecord = currentTranData.getCurrentSettlementRecord();
        Iterator<SettlementRecordItem> iterator = settlementRecord.getSettlementRecordItems().iterator();
        PrintTask printTask = new PrintTask(taskId++);
        while (iterator.hasNext()) {
            SettlementRecordItem recordItem = iterator.next();
            if (!recordItem.isNeedSettlement() && !recordItem.isNeedSettlementButBatchEmpty()) {
                LogUtil.d(TAG, "Host[" + HostType.toDebugString(recordItem.getHostType()) + "] not require settlement");
                continue;
            }

            printInfo.setPrintConfig(iRepository.getPrintConfig(recordItem.getMerchantIndex()));

            //printLogo(printTask, printInfo);
           /* if (printInfo.isDuplicateSlip()) {
                addDuplicateLabel(printTask);
            }*/
            addSettleHostMerchantHeader(printInfo, printTask, recordItem);

            addOneBlackLine(printTask);
            if (recordItem.isEmptyBatch(false)) {
                addOneDividingLine(printTask, '-');
                addOneBlackLine(printTask);
                printTask.addPrintLine(PrinterParamIn.FONT_SMALL, PrinterParamIn.ALIGN_CENTER, false, "- - - - Empty Batch - - - -");
                addOneBlackLine(printTask);
                addOneDividingLine(printTask, '-');
            } else if (!recordItem.isSettlementSuccess()) {
                addOneDividingLine(printTask, '-');
                addOneBlackLine(printTask);
                printTask.addPrintLine(PrinterParamIn.FONT_SMALL, PrinterParamIn.ALIGN_CENTER, false, "- - - - Settlement Failed - - - -");
                addOneBlackLine(printTask);
                addOneDividingLine(printTask, '-');
            } else {
                List<SettlePrintItem> settlePrintItems = recordItem.getSettlePrintItemList();
                if (settlePrintItems.size() > 0) {
                  /*  printTask.addTwoTextPrintLine("CARD TYPE", "CARD NUMBER");
                    printTask.addTwoTextPrintLine("EXP DATE", "INVOICE NUM");
                    printTask.addTwoTextPrintLine("TRANSACTION", "AMOUNT");
                    printTask.addTwoTextPrintLine("APPR CODE", "DATE/TIME");
                    addOneDividingLine(printTask, '-');
                    for(int i = 0; i < settlePrintItems.size(); i++) {
                        if (i != 0 && i % 10 == 0) {
                            addSettlePrintInfoToPrinter(printTask, emitter, false);
                            printTask = new PrintTask(taskId++);
                        }
                        addOneSettleRecord(printTask, settlePrintItems.get(i));
                    }
                    */

                    int[] cardTypeList = CardType.getCardTypeList();
                    for (int i = 0; i < cardTypeList.length; i++) {
                        addOneCardTypeSettleInfo(printTask, settlePrintItems, recordItem.getHostType(), cardTypeList[i]);
                    }
                    addOneBlackLine(printTask);
                    addGrandTotalSettleInfo(printTask, settlePrintItems, recordItem.getHostType());
                } else {
                    LogUtil.e(TAG, "No Print Item Found.");
                }
            }

            addSettlePrintInfoToPrinter(printTask, emitter, false);
            printTask = new PrintTask(taskId++);
        }

        addOneBlackLine(printTask);
        printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, PrinterParamIn.ALIGN_CENTER, true, "SETTLEMENT SUCCESSFUL");
        addOneBlackLine(printTask);
        addOneBlackLine(printTask);
        addOneBlackLine(printTask);
        addSettlePrintInfoToPrinter(printTask, emitter, true);
    }

    private void printLogo(PrintTask printTask, PrintInfo printInfo) {

        if (printInfo.getPrintLogoData() != null) {
            printTask.addPrintImage(0, 146, 384, printInfo.getPrintLogoData());
        }
    }

    //    private void printDetailReportSlip(ObservableEmitter emitter, PrintInfo printInfo) {
//        int taskId = 0;
//        SettlementRecord settlementRecord = iRepository.getSettlementInformation();
//        Iterator<SettlementRecordItem> iterator = settlementRecord.getSettlementRecordItems().iterator();
//        PrintTask printTask = new PrintTask(taskId++);
//        while (iterator.hasNext()) {
//            SettlementRecordItem recordItem = iterator.next();
//
//            printInfo.setPrintConfig(iRepository.getPrintConfig(recordItem.getMerchantIndex()));
//            // printLogo(printTask, printInfo);
//            addOneBlackLine(printTask);
//            //  addHeader(printTask, printInfo);
//            //addDuplicateLabel(printTask);
//            addSettleHostMerchantHeader(printInfo, printTask, recordItem);
//
//            addOneBlackLine(printTask);
//            if (recordItem.isEmptyBatch(false)) {
//                printTask.addPrintLine(PrinterParamIn.FONT_SMALL, PrinterParamIn.ALIGN_CENTER, false, "Empty Batch");
//                addOneDividingLine(printTask, '-');
//            } else {
//                List<SettlePrintItem> settlePrintItems = recordItem.getSettlePrintItemList();
//                if (settlePrintItems.size() > 0) {
//                    printTask.addTwoTextPrintLine("CARD TYPE", "CARD NUMBER");
//                    printTask.addTwoTextPrintLine("EXP DATE", "INVOICE NUM");
//                    printTask.addTwoTextPrintLine("TRANSACTION", "AMOUNT");
//                    printTask.addTwoTextPrintLine("APPR CODE", "DATE/TIME");
//                    addOneDividingLine(printTask, '-');
//                    for(int i = 0; i < settlePrintItems.size(); i++) {
//                        if (i != 0 && i % 10 == 0) {
//                            addSettlePrintInfoToPrinter(printTask, emitter, false);
//                            printTask = new PrintTask(taskId++);
//                        }
//                        addOneSettleRecord(printTask, settlePrintItems.get(i));
//                    }
//                }
//            }
//
//            addSettlePrintInfoToPrinter(printTask, emitter, false);
//            printTask = new PrintTask(taskId++);
//        }
//
//        addOneBlackLine(printTask);
//        addOneBlackLine(printTask);
//        addOneBlackLine(printTask);
//        addSettlePrintInfoToPrinter(printTask, emitter, true);
//    }
    private void printDetailReportSlip(ObservableEmitter emitter, PrintInfo printInfo) {
        int taskId = 0;
        SettlementRecord settlementRecord = iRepository.getSettlementInformation();
        Iterator<SettlementRecordItem> iterator = settlementRecord.getSettlementRecordItems().iterator();
        PrintTask printTask = new PrintTask(taskId++);
        while (iterator.hasNext()) {
            SettlementRecordItem recordItem = iterator.next();

            printInfo.setPrintConfig(iRepository.getPrintConfig(recordItem.getMerchantIndex()));

            addLogoImage(printTask, printInfo);
            //  addOneBlackLine(printTask);
            // addHeader(printTask, printInfo);

            addOneBlackLine(printTask);
            // addDuplicateLabel(printTask);
            addSettleHostMerchantHeader(printInfo, printTask, recordItem);

            addOneBlackLine(printTask);
            if (recordItem.isEmptyBatch(false)) {
                printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, PrinterParamIn.ALIGN_CENTER, false, "Empty Batch");
                addOneDividingLine(printTask, '-');
            } else {
                List<SettlePrintItem> settlePrintItems = recordItem.getSettlePrintItemList();
                if (settlePrintItems.size() > 0) {
                    printTask.addTwoLine("CARD TYPE", "CARD NUMBER");
                    printTask.addTwoLine("EXP DATE ", "INVOICE NUM");
                    printTask.addTwoLine("TRANSACTION", "AMOUNT");
                    printTask.addTwoLine("APPR CODE ", "DATE/TIME");

                    addOneDividingLine(printTask, '-');
                    for (int i = 0; i < settlePrintItems.size(); i++) {
                        if (i != 0 && i % 10 == 0) {
                            addSettlePrintInfoToPrinter(printTask, emitter, false);
                            printTask = new PrintTask(taskId++);
                        }
                        addOneSettleRecord(printTask, settlePrintItems.get(i));
                    }
                }
            }

            addSettlePrintInfoToPrinter(printTask, emitter, false);
            printTask = new PrintTask(taskId++);
        }

        addOneBlackLine(printTask);
        addOneBlackLine(printTask);
        addOneBlackLine(printTask);
        addSettlePrintInfoToPrinter(printTask, emitter, true);
    }

    private void printSummaryReportSlip(ObservableEmitter emitter, PrintInfo printInfo) {
        int taskId = 0;
        SettlementRecord settlementRecord = iRepository.getSettlementInformation();
        Iterator<SettlementRecordItem> iterator = settlementRecord.getSettlementRecordItems().iterator();
        PrintTask printTask = new PrintTask(taskId++);
        while (iterator.hasNext()) {
            SettlementRecordItem recordItem = iterator.next();

            printInfo.setPrintConfig(iRepository.getPrintConfig(recordItem.getMerchantIndex()));

            // printLogo(printTask, printInfo);
            // addOneBlackLine(printTask);
            // addHeader(printTask, printInfo);
            // addDuplicateLabel(printTask);
            addSettleHostMerchantHeader(printInfo, printTask, recordItem);

            addOneBlackLine(printTask);
            if (recordItem.isEmptyBatch(false)) {
                printTask.addPrintLine(PrinterParamIn.FONT_SMALL, PrinterParamIn.ALIGN_CENTER, false, "Empty Batch");
                addOneDividingLine(printTask, '-');
            } else {
                List<SettlePrintItem> settlePrintItems = recordItem.getSettlePrintItemList();
                if (settlePrintItems.size() > 0) {
                    int[] cardTypeList = CardType.getCardTypeList();
                    for (int i = 0; i < cardTypeList.length; i++) {
                        addOneCardTypeSettleInfo(printTask, settlePrintItems, recordItem.getHostType(), cardTypeList[i]);
                    }
                    addOneBlackLine(printTask);
                    addGrandTotalSettleInfo(printTask, settlePrintItems, recordItem.getHostType());
                }
            }

            addSettlePrintInfoToPrinter(printTask, emitter, false);
            printTask = new PrintTask(taskId++);
        }

        addOneBlackLine(printTask);
        addOneBlackLine(printTask);
        addOneBlackLine(printTask);
        addSettlePrintInfoToPrinter(printTask, emitter, true);
    }

    private void addSettlePrintInfoToPrinter(PrintTask printTask, ObservableEmitter emitter, boolean isLastTask) {
        if (isLastTask) {
            printTask.setOnlyNotifyPrintError(false);
        } else {
            printTask.setOnlyNotifyPrintError(true);
        }
        LogUtil.d(TAG, "===addSettlePrintInfoToPrinter taskId[" + printTask.getTaskId() + "]");
        printTaskManager.addPrintTask(printTask, new IPrintListener() {
            @Override
            public void onSuccess(int taskId) {
                emitter.onNext(taskId);
                emitter.onComplete();
            }

            @Override
            public void onError(Throwable throwable) {
                emitter.onError(throwable);
            }
        });
    }

    private void addSettleHostMerchantHeader(PrintInfo printInfo, PrintTask printTask, SettlementRecordItem recordItem) {
        SimpleDateFormat transTime = new SimpleDateFormat("HHmmss", Locale.getDefault());
        SimpleDateFormat transDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        Date date = new Date();
        String settlementTime = StringUtil.formatTime(transTime.format(date));
        String settlementDate = StringUtil.formatDate(transDate.format(date));
        addLogoImage(printTask, printInfo);
        printLogo(printTask, printInfo);
        addHeader(printTask, printInfo);
        // printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, String.format("DATE/TIME    : %s", settlementDate), settlementTime);
        printTask.addInlinePrint(String.format("DATE:%s", settlementDate), "", String.format("TIME:%s", settlementTime), PrinterParamIn.FONT_NORMAL, false);
        MerchantInfo merchantInfo = iRepository.getMerchantInfo(recordItem.getMerchantIndex());
        // printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, String.format("TERM/MERCH ID: %s", merchantInfo.getTerminalId()), merchantInfo.getMerchantId());
        printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, ALIGN_LEFT, true, "TID:" + merchantInfo.getTerminalId() + "      MID:" + merchantInfo.getMerchantId());
        printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, String.format("BATCH       #: %s", recordItem.getBatchNum()));
        HostInfo hostInfo = iRepository.getHostInfoByHostType(recordItem.getHostType());
        //printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, String.format("HOST        #: %s", hostInfo.getHostName()));
        switch (printInfo.getPrintType()) {
            case PrintType.SETTLEMENT:
                printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, PrinterParamIn.ALIGN_CENTER, true, "SETTLEMENT REPORT");
                break;
            case PrintType.SUMMARY_REPORT:
                printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, PrinterParamIn.ALIGN_CENTER, true, "SUMMARY REPORT");
                break;
            case PrintType.DETAIL_REPORT:
                printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, PrinterParamIn.ALIGN_CENTER, true, "DETAIL REPORT");
                break;
        }
    }

    private void addOneSettleRecord(PrintTask printTask, SettlePrintItem settlePrintItem) {
        String d = settlePrintItem.getApprovalCode() + "                       " + settlePrintItem.getDateTime();

        printTask.addTwoLine(settlePrintItem.getCardTypeAbbr(), getPrintPanText(settlePrintItem.getPan(), settlePrintItem.getCardEntryMode()));
        printTask.addTwoLine("**/**", settlePrintItem.getInvoiceNum());
        printTask.addTwoLine(settlePrintItem.getTransaction(), settlePrintItem.getCurrencyAmountText());

        printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, d);

        addOneBlackLine(printTask);
    }

    private void addOneCardTypeSettleInfo(PrintTask printTask, List<SettlePrintItem> settlePrintItems, int hostType, int cardType) {
        int saleCount = 0;
        int EmiCount = 0;
        int CashAdvanceCount = 0;
        int voidSaleCount = 0;
        int preAuthCompCount = 0;
        int voidPreAuthCompCount = 0;
        long saleAmountTotal = 0;
        long emiTotal = 0;
        long voidSaleAmountTotal = 0;
        long preAuthCompAmountTotal = 0;
        long voidPreAuthCompAmountTotal = 0;
        long CashAdvanceAmountToltal = 0;

        Iterator<SettlePrintItem> iterator = settlePrintItems.iterator();
        while (iterator.hasNext()) {

            SettlePrintItem item = iterator.next();

            if (item.getCardType() == cardType) {
                switch (item.getRecordType()) {
                    case SettlePrintItem.SALE:
                        saleCount++;
                        saleAmountTotal += item.getTotalAmountLong();

                        break;
                    case SettlePrintItem.VOID_SALE:
                        voidSaleCount++;
                        voidSaleAmountTotal += item.getTotalAmountLong();
                        break;
                    case SettlePrintItem.PREAUTH_COMP:
                        preAuthCompCount++;
                        preAuthCompAmountTotal += item.getTotalAmountLong();
                        break;
                    case SettlePrintItem.VOID_PREAUTH_COMP:
                        voidPreAuthCompCount++;
                        voidPreAuthCompAmountTotal += item.getTotalAmountLong();
                        break;
                    case SettlePrintItem.INSTALLMENT:
                        EmiCount++;
                        emiTotal += item.getTotalAmountLong();
                        break;
                    case SettlePrintItem.CASH_ADV:
                        CashAdvanceCount++;
                        CashAdvanceAmountToltal += item.getTotalAmountLong();
                        break;

                }
            }
        }

        if (saleCount + voidSaleCount + preAuthCompCount + voidPreAuthCompCount + EmiCount + CashAdvanceCount > 0) {
            addOneDividingLine(printTask, '-');
            printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, "CARDTYPE :" + CardType.toCardTypeText(cardType));
            //  printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, "TRANS          COUNT", "AMT");
            printTask.addThreeLine("TRANS", "COUNT", "AMT");
//            printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, "TRANS                  COUNT       AMT");
            //  printTask.addCustomThreeColPrintLine(PrinterParamIn.FONT_NORMAL, true,"TRANS", "COUNT","AMT");
            //addOneBlackLine(printTask);
            if (hostType == HostType.INSTALLMENT) {
                printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, "INSTALLMENT          " + getCountText(saleCount) + StringUtil.formatAmount("" + saleAmountTotal));
                printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, "VOID INST           " + getCountText(voidSaleCount), StringUtil.formatAmount("" + voidSaleAmountTotal));
            } else {
                if (saleCount > 0) {
                    printTask.addThreeLine("SALE", getCountText(saleCount), String.valueOf((float) saleAmountTotal / 100));

                }
                if (voidSaleCount > 0) {
                    printTask.addThreeLine("VOID SALE", getCountText(voidSaleCount), "-"+String.valueOf((float) voidSaleAmountTotal / 100));

                }
                if (preAuthCompCount > 0) {
                    printTask.addThreeLine("PREAUTH COMP ", getCountText(preAuthCompCount), String.valueOf((float) preAuthCompAmountTotal / 100));

                }
                if (voidPreAuthCompCount > 0) {
                    printTask.addThreeLine("VOID PRECOMP ", getCountText(voidPreAuthCompCount), String.valueOf((float) voidPreAuthCompAmountTotal / 100));

                }
                if (EmiCount > 0) {
                    printTask.addThreeLine("EMI SALE ", getCountText(EmiCount), String.valueOf((float) emiTotal / 100));

                }
                if (CashAdvanceCount > 0) {
                    printTask.addThreeLine("CASH ADVANCE ", getCountText(CashAdvanceCount), String.valueOf((float) CashAdvanceAmountToltal / 100));

                }
                addOneDividingLine(printTask, '-');
                //   printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, "SALE                        " + getCountText(saleCount) + "               " + saleAmountTotal);
                //   printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, "VOID SALE             " + getCountText(voidSaleCount) + "               " + voidSaleAmountTotal);
                //  printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, "PREAUTH COMP " + getCountText(preAuthCompCount) + "               " + preAuthCompAmountTotal);
                //  printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, "VOID PRECOMP  " + getCountText(voidPreAuthCompCount) + "               " + voidPreAuthCompAmountTotal);

//                printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, "SALE                " + getCountText(saleCount), StringUtil.formatAmount("" + saleAmountTotal));
//                printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, "VOID SALE           " + getCountText(voidSaleCount), StringUtil.formatAmount("" + voidSaleAmountTotal));
//                printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, "PREAUTH COMP        " + getCountText(preAuthCompCount), StringUtil.formatAmount("" + preAuthCompAmountTotal));
//                printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, "VOID PRECOMP        " + getCountText(voidPreAuthCompCount), StringUtil.formatAmount("" + voidPreAuthCompAmountTotal));
//                printTask.addCustomThreeColPrintLine(PrinterParamIn.FONT_NORMAL, false, "SALE       ", getCountText(saleCount), StringUtil.formatAmount("" + saleAmountTotal));
//                printTask.addCustomThreeColPrintLine(PrinterParamIn.FONT_NORMAL, false, "VOID SALE  ", getCountText(voidSaleCount), StringUtil.formatAmount("" + voidSaleAmountTotal));
//                printTask.addCustomThreeColPrintLine(PrinterParamIn.FONT_NORMAL, false, "PREAUTH COMP", getCountText(preAuthCompCount), StringUtil.formatAmount("" + preAuthCompAmountTotal));
//                printTask.addCustomThreeColPrintLine(PrinterParamIn.FONT_NORMAL, false, "VDAUTH COMP", getCountText(voidPreAuthCompCount), StringUtil.formatAmount("" + voidPreAuthCompAmountTotal));
                long totalAmount=saleAmountTotal+CashAdvanceAmountToltal+emiTotal;
                printTask.addThreeLine("", "TOTAL", String.valueOf((float) totalAmount / 100));

            }
            addOneBlackLine(printTask);
        }
    }

    private void addGrandTotalSettleInfo(PrintTask printTask, List<SettlePrintItem> settlePrintItems, int hostType) {
        int saleCount = 0;
        int voidSaleCount = 0;
        int preAuthCompCount = 0;
        int voidPreAuthCompCount = 0;
        long saleAmountTotal = 0;
        long voidSaleAmountTotal = 0;
        long preAuthCompAmountTotal = 0;
        long voidPreAuthCompAmountTotal = 0;
        int EmiCount = 0;
        long emiTotal = 0;
        int CashAdvanceCount = 0;
        long CashAdvanceAmountToltal = 0;
        Iterator<SettlePrintItem> iterator = settlePrintItems.iterator();
        while (iterator.hasNext()) {
            SettlePrintItem item = iterator.next();
            switch (item.getRecordType()) {
                case SettlePrintItem.SALE:
                    saleCount++;
                    saleAmountTotal += item.getTotalAmountLong();
                    break;

                case SettlePrintItem.VOID_SALE:
                    voidSaleCount++;
                    voidSaleAmountTotal += item.getTotalAmountLong();
                    break;
                case SettlePrintItem.PREAUTH_COMP:
                    preAuthCompCount++;
                    preAuthCompAmountTotal += item.getTotalAmountLong();
                    break;
                case SettlePrintItem.VOID_PREAUTH_COMP:
                    voidPreAuthCompCount++;
                    voidPreAuthCompAmountTotal += item.getTotalAmountLong();
                    break;
                case SettlePrintItem.INSTALLMENT:
                    EmiCount++;
                    emiTotal += item.getTotalAmountLong();
                    break;
                case SettlePrintItem.CASH_ADV:
                    CashAdvanceCount++;
                    CashAdvanceAmountToltal += item.getTotalAmountLong();
                    break;
            }

        }

        printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, true, "GRAND TOTAL", "");
        // printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, "TRANS                  CNT", "AMT");
        // printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, "TRANS                  COUNT       AMT");
        printTask.addThreeLine("TRANS", "COUNT", "AMT");
        if (hostType == HostType.INSTALLMENT) {
            printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, "INSTALLMENT            " + getCountText(saleCount), StringUtil.formatAmount("" + saleAmountTotal));
            printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, "VOID INST              " + getCountText(voidSaleCount), StringUtil.formatAmount("" + voidSaleAmountTotal));
        } else {
            if (saleCount > 0) {
                printTask.addThreeLine("SALE", getCountText(saleCount), String.valueOf((float) saleAmountTotal / 100));

            }
            if (voidSaleCount > 0) {
                printTask.addThreeLine("VOID SALE", getCountText(voidSaleCount), "-"+String.valueOf((float) voidSaleAmountTotal / 100));

            }
            if (preAuthCompCount > 0) {
                printTask.addThreeLine("PREAUTH COMP ", getCountText(preAuthCompCount), String.valueOf((float) preAuthCompAmountTotal / 100));

            }
            if (voidPreAuthCompCount > 0) {
                printTask.addThreeLine("VOID PRECOMP ", getCountText(voidPreAuthCompCount), String.valueOf((float) voidPreAuthCompAmountTotal / 100));

            }
            if (EmiCount > 0) {
                printTask.addThreeLine("EMI SALE ", getCountText(EmiCount), String.valueOf((float) emiTotal / 100));

            }
            if (CashAdvanceCount > 0) {
                printTask.addThreeLine("CASH ADVANCE ", getCountText(CashAdvanceCount), String.valueOf((float) CashAdvanceAmountToltal / 100));

            }

        }
        addOneDividingLine(printTask, '-');
        long totalAmount=saleAmountTotal+CashAdvanceAmountToltal+emiTotal;
        printTask.addThreeLine("", "TOTAL", String.valueOf((float) totalAmount / 100));

        addOneBlackLine(printTask);
    }

    private String getCountText(int count) {
        if (count >= 100) {
            return "" + count;
        } else if (count >= 10) {
            return " " + count;
        } else {
            return " " + count + " ";
        }
    }

    private PrintTask getPrintTask(PrintInfo printInfo) {
        Log.e("T_CHECK_002", printInfo.getPrintType() + "");
        switch (printInfo.getPrintType()) {
            case PrintType.LOGON:
                return new LogonPrintTask(recordInfo, printInfo);
            case PrintType.SALE:
            case PrintType.PREAUTH:
            case PrintType.PREAUTH_COMP:
            case PrintType.OFFLINE:
            case PrintType.TIP_ADJUST:
                return new CommonPrintTask(recordInfo, printInfo);
            case PrintType.INSTALLMENT:
                return new InstallmentPrintTask(recordInfo, printInfo);
            case PrintType.VOID:
                return new VoidPrintTask(recordInfo, printInfo);

        }


        return new CommonPrintTask(recordInfo, printInfo);
    }

    private void startPrintTask(ObservableEmitter emitter, PrintTask printTask) {

    }


    private void addLogoImage(PrintTask printTask, PrintInfo printInfo) {
        if (printInfo.getPrintLogoData() != null) {
            printTask.addPrintImage(100, 100, 384, printInfo.getPrintLogoData());
        }
    }

    private void addHeader(PrintTask printTask, PrintInfo printInfo) {
        PrintConfig printConfig = printInfo.getPrintConfig();
        if (printConfig != null) {
            if (StringUtil.getNonNullString(printConfig.getHeader1()).length() > 0) {
                printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, PrinterParamIn.ALIGN_CENTER, true, printConfig.getHeader1());
            }
            if (StringUtil.getNonNullString(printConfig.getHeader2()).length() > 0) {
                printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, PrinterParamIn.ALIGN_CENTER, false, printConfig.getHeader2());
            }
            if (StringUtil.getNonNullString(printConfig.getHeader3()).length() > 0) {
                printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, PrinterParamIn.ALIGN_CENTER, false, printConfig.getHeader3());
            }
            if (StringUtil.getNonNullString(printConfig.getHeader4()).length() > 0) {
                printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, PrinterParamIn.ALIGN_CENTER, false, printConfig.getHeader4());
            }
            if (StringUtil.getNonNullString(printConfig.getHeader5()).length() > 0) {
                printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, PrinterParamIn.ALIGN_CENTER, false, printConfig.getHeader5());
            }
            if (StringUtil.getNonNullString(printConfig.getHeader6()).length() > 0) {
                printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, PrinterParamIn.ALIGN_CENTER, false, printConfig.getHeader6());
            }
        }
    }

    private void addDuplicateLabel(PrintTask printTask) {
        final int bgHeight = 40;
        final int bgWidth = 380;
        final int fontSize = 30;
        Bitmap bitmap = Bitmap.createBitmap(bgWidth, bgHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.BLACK);

        Paint paint = new Paint();
        paint.setTextSize(fontSize);
        paint.setColor(Color.WHITE);
        float textWidth = paint.measureText("DUPLICATE");
        float baseLineY = bgHeight / 2 - (paint.ascent() + paint.descent()) / 2;
        canvas.drawText("DUPLICATE", bgWidth / 2 - textWidth / 2, baseLineY, paint);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(bitmap.getByteCount());
        bitmap.compress(Bitmap.CompressFormat.JPEG, 10, bos); // 40 is ok, I tested it.
        printTask.addPrintImage(0, bgHeight, bgWidth, bos.toByteArray());
    }

    private void addOneBlackLine(PrintTask printTask) {
        printTask.addPrintLine(PrinterParamIn.FONT_NORMAL, " ");
    }

    private void addOneDividingLine(PrintTask printTask, char dividingChar) {
//        printTask.addPrintLine(PrinterParamIn.FONT_SMALL, "- - - - - - - - - - - - - - - - - - - - - - - - - - -");
        printTask.addPrintLine(PrinterParamIn.FONT_SMALL, "------------------------------------------");

    }

    private String getPrintPanText(String pan, int cardEntryMode) {
        String panLast4Digit = pan.substring(pan.length() - 4);
        String panLast6Digit = pan.substring(0, 6);

        String entryModeText = "";
        switch (cardEntryMode) {
            case CardEntryMode.IC:
            case CardEntryMode.RF:
                entryModeText = "C";
                break;
            case CardEntryMode.MAG:
                entryModeText = "S";
                break;
            case CardEntryMode.MANUAL:
                entryModeText = "M";
                break;
        }
        return String.format("%s***%s(%s)", panLast6Digit, panLast4Digit, entryModeText);
    }
}
