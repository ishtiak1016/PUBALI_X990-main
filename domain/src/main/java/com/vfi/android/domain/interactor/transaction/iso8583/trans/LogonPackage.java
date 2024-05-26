package com.vfi.android.domain.interactor.transaction.iso8583.trans;

import android.util.Log;

import com.vfi.android.domain.entities.consts.TAGS;
import com.vfi.android.domain.entities.exceptions.CommonException;
import com.vfi.android.domain.interactor.transaction.iso8583.base.AbsTransPackage;
import com.vfi.android.domain.interactor.transaction.iso8583.base.ITransPackage;
import com.vfi.android.domain.interactor.transaction.iso8583.base.TransPackageData;
public class LogonPackage extends AbsTransPackage implements ITransPackage {
    private static final String TAG = TAGS.ISO_8583;

    public LogonPackage(TransPackageData transPackageData) {
        super(transPackageData);
        Log.d("iswhtiak", String.valueOf(transPackageData.getTransType()));

    }

    @Override
    protected int[] getTransFields() {
        // 52, 61 for cup Credit/Debit
        return new int[] {0,3,11, 24, 41, 42, 62};
    }

    @Override
    public byte[] packISO8583Message() throws CommonException {
        return packISOMessage();
    }

    @Override
    public String f2_pan() {
        String track2 = getRecordInfo().getTrack2();
        if (track2 == null) {
            return getRecordInfo().getPan();
        } else {
            return null;
        }
    }

    @Override
    public String f3_processCode() {
        return getRecordInfo().getProcessCode();
    }

    @Override
    public String f14_expireDate() {
        String track2 = getRecordInfo().getTrack2();
        if (track2 == null) {
            return getRecordInfo().getCardExpiryDate();
        } else {
            return null;
        }
    }

    @Override
    public void unPackISO8583Message(byte[] message) throws CommonException {
        unPackISOMessage(message);
    }
}