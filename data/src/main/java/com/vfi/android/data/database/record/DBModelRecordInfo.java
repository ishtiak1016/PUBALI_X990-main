package com.vfi.android.data.database.record;


import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.vfi.android.data.database.DBFlowDatabase;

/**
 * Created by RuihaoS on 2019/8/13.
 */
@Table(database = DBFlowDatabase.class)
public class DBModelRecordInfo extends BaseModel {
    /**
     * transaction type
     */
    @Column
    private int transType;

    /**
     * void original transaction type
     */
    @Column
    private int voidOrgTransType;

    /**
     * tipAdjust original transaction type
     */
    @Column
    private int tipAdjOrgTransType;

    /**
     * Reversal original transaction type.
     */
    @Column
    private int reversalOrgTransType;

    /**
     * field 02 account
     */
    @Column
    private String pan;

    /**
     * field 03 Processing code
     */
    @Column
    private String processCode;

    /**
     * field 04 amount
     */
    @Column
    private String amount;

    /**
     * TIP or cash back
     */
    @Column
    private String tipAmount;

    /**
     * Currency amount, such as dcc amount
     */
    @Column
    private String currencyAmount;

    /**
     * Field 11 system trace
     */
    @Column
    @PrimaryKey
    private String traceNum;

    /**
     * Field 11 original system trace
     */
    @Column
    private String voidOrgTraceNum;

    /**
     * Field 11 original system trace
     */
    @Column
    private String tipAdjOrgTraceNum;

    /**
     * HostInfo INDEX
     */
    @Column
    private int hostType;

    /**
     * CardBinInfo INDEX
     */
    @Column
    private int cardBinIndex;

    /**
     * merchant INDEX
     */
    @Column
    private int merchantIndex;

    /**
     * currency INDEX
     */
    @Column
    private int currencyIndex;

    /**
     * field 62 invoice hum
     */
    @Column
    @PrimaryKey
    private String invoiceNum;

    /**
     * field 62 invoice hum
     */
    @Column
    private String orgInvoiceNum;

    /**
     * Field 12 transaction time
     */
    @Column
    private String transTime;

    /**
     * Field 13 transaction date
     */
    @Column
    private String transDate;

    /**
     * Field 13 original transaction date
     */
    @Column
    private String orgTransDate;

    /**
     * Field 14 valid period of card
     */
    @Column
    private String cardExpiryDate;

    /**
     * Field 15 清算日期(format:MMSS)
     */
    @Column
    private String batchSettleDate;

    /**
     * Field 23 Card serial number
     */
    @Column
    private String cardSequenceNum;

    /**
     * Field 22 Pos entry mode
     */
    @Column
    private String posEntryMode;

    /**
     * Field 24 Network international ID
     */
    @Column
    private String nii;

    /**
     * Field 35 Track2 information
     */
    @Column
    private String track2;

    /**
     * Field 36 Track3 information
     */
    @Column
    private String track3;

    /**
     * Field 37 reference number
     */
    @Column
    private String refNo;

    /**
     * Field 37 org reference number
     */
    @Column
    private String orgRefNo;

    /**
     * Field 38 auth code
     */
    @Column
    private String authCode;

    /**
     * Field 38 org auth code
     */
    @Column
    private String orgAuthCode;

    /**
     * Field 39 Response Code
     */
    @Column
    private String rspCode;

    /**
     * terminalId
     */
    @Column
    private String terminalId;

    /**
     * merchantId
     */
    @Column
    private String merchantId;

    /**
     * Field 55
     */
    @Column
    private String field55;

    /**
     * Field 55 for TC
     */
    @Column
    private String field55ForTC;

    /**
     * Field 60 batch number
     */
    @Column
    @PrimaryKey
    private String batchNo;

    /**
     * Field 61 org batch number
     */
    @Column
    private String orgBatchNo;

    /**
     * Field 63.1  card organizationName
     */
    @Column
    private String cardOrgCode;

    /**
     * 63.2 Note from Host
     */
    @Column
    private String hostNote;

    /**
     * IC card transaction data, cardholder information
     */
    @Column
    private String cardHolderName;

    /**
     * app label tag 50
     */
    @Column
    private String AppLabel;

    /**
     * aid tag 84
     */
    @Column
    private String AID;

    /**
     * Field 49 CurrencyData 5F2A
     */
    @Column
    private String CurrencyCode;

    /**
     * Whether to downgrade FALLBACK transactions
     */
    @Column
    private boolean isFallBack;

    /**
     * CVV2 for enter
     */
    @Column
    private String cvv2;

    /**
     * Card Type, {@link com.vfi.android.domain.entities.consts.CardType}
     */
    @Column
    private int cardType;

    /**
     * TC, 9F26
     */
    @Column
    private String tc;

    /**
     * When trans have TC , after upload to host, need mark this flag to true.
     */
    @Column
    private boolean isTcUploaded;

    /**
     * true - Emv offline approved the transaction.
     */
    @Column
    private boolean isEmvOfflineApproval;

    /**
     * When trans offline approved, after upload to host, need mark this flag to true.
     */
    @Column
    private boolean isOfflineTransUploaded;

    /**
     * Current transaction tipAdjust times, max adjust times: TerminalConfig->maxAdjustTimes
     */
    @Column
    private int tipAdjustTimes;

    /**
     * please refer {@link com.vfi.android.domain.entities.consts.CVMResult}
     */
    @Column
    private int cvmResult;

    /**
     * Zip format signature data.
     */
    @Column
    private byte[] signData;

    /**
     * Installment field 61, Installment plan (Type of Installment Payment Scheme), 3 byte
     */
    @Column
    private String promoCode;

    /**
     * Installment field 61, The number of terms, 2 byte number
     */
    @Column
    private int installmentTerm;

    /**
     * Below are installment print info, received from ISO message field 61
     */
    @Column
    private String promoLabel;
    @Column
    private String installmentFactorRate;
    @Column
    private String totalAmountDue;
    @Column
    private String monthlyDue;

    public int getTransType() {
        return transType;
    }

    public void setTransType(int transType) {
        this.transType = transType;
    }

    public int getVoidOrgTransType() {
        return voidOrgTransType;
    }

    public void setVoidOrgTransType(int voidOrgTransType) {
        this.voidOrgTransType = voidOrgTransType;
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public String getProcessCode() {
        return processCode;
    }

    public void setProcessCode(String processCode) {
        this.processCode = processCode;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getTipAmount() {
        return tipAmount;
    }

    public void setTipAmount(String tipAmount) {
        this.tipAmount = tipAmount;
    }

    public String getCurrencyAmount() {
        return currencyAmount;
    }

    public void setCurrencyAmount(String currencyAmount) {
        this.currencyAmount = currencyAmount;
    }

    public String getTraceNum() {
        return traceNum;
    }

    public void setTraceNum(String traceNum) {
        this.traceNum = traceNum;
    }

    public String getVoidOrgTraceNum() {
        return voidOrgTraceNum;
    }

    public void setVoidOrgTraceNum(String voidOrgTraceNum) {
        this.voidOrgTraceNum = voidOrgTraceNum;
    }

    public int getHostType() {
        return hostType;
    }

    public void setHostType(int hostType) {
        this.hostType = hostType;
    }

    public int getCardBinIndex() {
        return cardBinIndex;
    }

    public void setCardBinIndex(int cardBinIndex) {
        this.cardBinIndex = cardBinIndex;
    }

    public int getMerchantIndex() {
        return merchantIndex;
    }

    public void setMerchantIndex(int merchantIndex) {
        this.merchantIndex = merchantIndex;
    }

    public int getCurrencyIndex() {
        return currencyIndex;
    }

    public void setCurrencyIndex(int currencyIndex) {
        this.currencyIndex = currencyIndex;
    }

    public String getInvoiceNum() {
        return invoiceNum;
    }

    public void setInvoiceNum(String invoiceNum) {
        this.invoiceNum = invoiceNum;
    }

    public String getOrgInvoiceNum() {
        return orgInvoiceNum;
    }

    public void setOrgInvoiceNum(String orgInvoiceNum) {
        this.orgInvoiceNum = orgInvoiceNum;
    }

    public String getTransTime() {
        return transTime;
    }

    public void setTransTime(String transTime) {
        this.transTime = transTime;
    }

    public String getTransDate() {
        return transDate;
    }

    public void setTransDate(String transDate) {
        this.transDate = transDate;
    }

    public String getOrgTransDate() {
        return orgTransDate;
    }

    public void setOrgTransDate(String orgTransDate) {
        this.orgTransDate = orgTransDate;
    }

    public String getCardExpiryDate() {
        return cardExpiryDate;
    }

    public void setCardExpiryDate(String cardExpiryDate) {
        this.cardExpiryDate = cardExpiryDate;
    }

    public String getBatchSettleDate() {
        return batchSettleDate;
    }

    public void setBatchSettleDate(String batchSettleDate) {
        this.batchSettleDate = batchSettleDate;
    }

    public String getCardSequenceNum() {
        return cardSequenceNum;
    }

    public void setCardSequenceNum(String cardSequenceNum) {
        this.cardSequenceNum = cardSequenceNum;
    }

    public String getNii() {
        return nii;
    }

    public void setNii(String nii) {
        this.nii = nii;
    }

    public String getTrack2() {
        return track2;
    }

    public void setTrack2(String track2) {
        this.track2 = track2;
    }

    public String getTrack3() {
        return track3;
    }

    public void setTrack3(String track3) {
        this.track3 = track3;
    }

    public String getRefNo() {
        return refNo;
    }

    public void setRefNo(String refNo) {
        this.refNo = refNo;
    }

    public String getOrgRefNo() {
        return orgRefNo;
    }

    public void setOrgRefNo(String orgRefNo) {
        this.orgRefNo = orgRefNo;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getOrgAuthCode() {
        return orgAuthCode;
    }

    public void setOrgAuthCode(String orgAuthCode) {
        this.orgAuthCode = orgAuthCode;
    }

    public String getRspCode() {
        return rspCode;
    }

    public void setRspCode(String rspCode) {
        this.rspCode = rspCode;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getField55() {
        return field55;
    }

    public void setField55(String field55) {
        this.field55 = field55;
    }

    public String getField55ForTC() {
        return field55ForTC;
    }

    public void setField55ForTC(String field55ForTC) {
        this.field55ForTC = field55ForTC;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getOrgBatchNo() {
        return orgBatchNo;
    }

    public void setOrgBatchNo(String orgBatchNo) {
        this.orgBatchNo = orgBatchNo;
    }

    public String getCardOrgCode() {
        return cardOrgCode;
    }

    public void setCardOrgCode(String cardOrgCode) {
        this.cardOrgCode = cardOrgCode;
    }

    public String getHostNote() {
        return hostNote;
    }

    public void setHostNote(String hostNote) {
        this.hostNote = hostNote;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public boolean isEmvOfflineApproval() {
        return isEmvOfflineApproval;
    }

    public void setEmvOfflineApproval(boolean emvOfflineApproval) {
        isEmvOfflineApproval = emvOfflineApproval;
    }

    public String getAppLabel() {
        return AppLabel;
    }

    public void setAppLabel(String appLabel) {
        AppLabel = appLabel;
    }

    public String getAID() {
        return AID;
    }

    public void setAID(String AID) {
        this.AID = AID;
    }

    public String getCurrencyCode() {
        return CurrencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        CurrencyCode = currencyCode;
    }

    public boolean isFallBack() {
        return isFallBack;
    }

    public void setFallBack(boolean fallBack) {
        isFallBack = fallBack;
    }

    public String getCvv2() {
        return cvv2;
    }

    public void setCvv2(String cvv2) {
        this.cvv2 = cvv2;
    }

    public String getPosEntryMode() {
        return posEntryMode;
    }

    public void setPosEntryMode(String posEntryMode) {
        this.posEntryMode = posEntryMode;
    }

    public boolean isTcUploaded() {
        return isTcUploaded;
    }

    public void setTcUploaded(boolean tcUploaded) {
        isTcUploaded = tcUploaded;
    }

    public boolean isOfflineTransUploaded() {
        return isOfflineTransUploaded;
    }

    public void setOfflineTransUploaded(boolean offlineTransUploaded) {
        isOfflineTransUploaded = offlineTransUploaded;
    }

    public int getTipAdjustTimes() {
        return tipAdjustTimes;
    }

    public void setTipAdjustTimes(int tipAdjustTimes) {
        this.tipAdjustTimes = tipAdjustTimes;
    }

    public int getTipAdjOrgTransType() {
        return tipAdjOrgTransType;
    }

    public void setTipAdjOrgTransType(int tipAdjOrgTransType) {
        this.tipAdjOrgTransType = tipAdjOrgTransType;
    }

    public String getTipAdjOrgTraceNum() {
        return tipAdjOrgTraceNum;
    }

    public void setTipAdjOrgTraceNum(String tipAdjOrgTraceNum) {
        this.tipAdjOrgTraceNum = tipAdjOrgTraceNum;
    }

    public int getCardType() {
        return cardType;
    }

    public void setCardType(int cardType) {
        this.cardType = cardType;
    }

    public String getTc() {
        return tc;
    }

    public void setTc(String tc) {
        this.tc = tc;
    }

    public int getCvmResult() {
        return cvmResult;
    }

    public void setCvmResult(int cvmResult) {
        this.cvmResult = cvmResult;
    }

    public byte[] getSignData() {
        return signData;
    }

    public void setSignData(byte[] signData) {
        this.signData = signData;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public int getInstallmentTerm() {
        return installmentTerm;
    }

    public void setInstallmentTerm(int installmentTerm) {
        this.installmentTerm = installmentTerm;
    }

    public String getPromoLabel() {
        return promoLabel;
    }

    public void setPromoLabel(String promoLabel) {
        this.promoLabel = promoLabel;
    }

    public String getInstallmentFactorRate() {
        return installmentFactorRate;
    }

    public void setInstallmentFactorRate(String installmentFactorRate) {
        this.installmentFactorRate = installmentFactorRate;
    }

    public String getTotalAmountDue() {
        return totalAmountDue;
    }

    public void setTotalAmountDue(String totalAmountDue) {
        this.totalAmountDue = totalAmountDue;
    }

    public String getMonthlyDue() {
        return monthlyDue;
    }

    public void setMonthlyDue(String monthlyDue) {
        this.monthlyDue = monthlyDue;
    }

    public int getReversalOrgTransType() {
        return reversalOrgTransType;
    }

    public void setReversalOrgTransType(int reversalOrgTransType) {
        this.reversalOrgTransType = reversalOrgTransType;
    }
}
