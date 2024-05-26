package com.vfi.android.domain.interactor.print.tasks;

import com.vfi.android.domain.entities.businessbeans.RecordInfo;
import com.vfi.android.domain.entities.databeans.PrintInfo;
import com.vfi.android.domain.entities.databeans.PrinterParamIn;
import com.vfi.android.domain.interactor.print.base.BaseTransPrintTask;
import com.vfi.android.domain.utils.StringUtil;

public class InstallmentPrintTask extends BaseTransPrintTask {
    public InstallmentPrintTask(RecordInfo recordInfo, PrintInfo printInfo) {
        super(recordInfo, printInfo);
    }

    @Override
    protected void addPrintContent() {
        addLogoImage();
        addOneBlackLine();
        addHeader();
        addOneBlackLine();
        if (getPrintInfo().isDuplicateSlip()) {
            addDuplicateLabel();
        }
        addDateTime();
        addTerminalIdMerchantId();
        addBatchInvoice();
        addOneBlackLine();
        addTransTypeLine();
        addOneBlackLine();
        addCardNum();
        addCardType();
        addApplicationLabel();
        addRRN();
        addApprovalCode();
        addAID();
        tvr();
        addTC();
        addOneBlackLine();
        addAmountLine();
        addInstallmentInfo();
        addOneBlackLine();
        addCVMResultLine();
        addCardHolderNameLine();
        addOneBlackLine();
        addDisclaimer();
        addSlipCopy();
        addFooter();
        addOneBlackLine();
        addOneBlackLine();
        addOneBlackLine();
    }

    private void addInstallmentInfo() {
        // addOneBlackLine();
        addOneBlackLine();
        RecordInfo recordInfo = getRecordInfo();
        //this.addPrintLine(PrinterParamIn.FONT_SMALL, false, "INST PLAN     :", recordInfo.getPromoLabel());
        //  this.addInlinePrint("INST PLAN: ","",recordInfo.getPromoLabel(),PrinterParamIn.FONT_NORMAL, false);
        //this.addPrintLine(PrinterParamIn.FONT_SMALL, false, "TERM          :", String.format("%02d", recordInfo.getInstallmentTerm()));
        this.addInlinePrint("TENURE: ", "", String.format("%02d", recordInfo.getInstallmentTerm()), PrinterParamIn.FONT_NORMAL, true);

        //  String factorRate = recordInfo.getInstallmentFactorRate().substring(0, 1) + "." + recordInfo.getInstallmentFactorRate().substring(1);
        //   this.addPrintLine(PrinterParamIn.FONT_SMALL, false, "FACTOR RATE   :", factorRate);
        String amount = StringUtil.formatAmount(recordInfo.getTotalAmountDue());
        amount = "PHP " + "         ".substring(amount.length()) + amount;
        // this.addPrintLine(PrinterParamIn.FONT_SMALL, false, "TOTAL AMT DUE :", amount);
        amount = StringUtil.formatAmount(recordInfo.getMonthlyDue());
        amount = "PHP " + "         ".substring(amount.length()) + amount;
        //   this.addPrintLine(PrinterParamIn.FONT_SMALL, false, "MONTHLY DUE   :", amount);
        addOneBlackLine();
        //addOneBlackLine();
    }
}
