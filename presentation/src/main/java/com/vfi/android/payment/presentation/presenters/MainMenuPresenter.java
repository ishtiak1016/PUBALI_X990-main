package com.vfi.android.payment.presentation.presenters;

import static com.vfi.android.payment.presentation.utils.ResUtil.getString;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.LogPrinter;
import android.util.Printer;
import android.util.Xml;
import android.view.View;
import android.widget.Toast;

import com.vfi.android.data.comm.network.SocketCommImpl;
import com.vfi.android.data.deviceservice.BindObservable;
import com.vfi.android.data.deviceservice.PinPadObservable;
import com.vfi.android.data.deviceservice.PosServiceImpl;
import com.vfi.android.domain.entities.businessbeans.CurrentTranData;
import com.vfi.android.domain.entities.businessbeans.DeviceInformation;
import com.vfi.android.domain.entities.businessbeans.HostInfo;
import com.vfi.android.domain.entities.businessbeans.RecordInfo;
import com.vfi.android.domain.entities.businessbeans.SwitchParameter;
import com.vfi.android.domain.entities.comm.SocketParam;
import com.vfi.android.domain.entities.consts.CTLSLedStatus;
import com.vfi.android.domain.entities.consts.InterceptorResult;
import com.vfi.android.domain.entities.consts.PrintType;
import com.vfi.android.domain.entities.consts.TAGS;
import com.vfi.android.domain.entities.consts.TransType;
import com.vfi.android.domain.entities.databeans.AppKeys;
import com.vfi.android.domain.entities.databeans.OperatorInfo;
import com.vfi.android.domain.entities.databeans.PinPadGeneralParamIn;
import com.vfi.android.domain.entities.databeans.PrintInfo;
import com.vfi.android.domain.entities.databeans.PrintTask;
import com.vfi.android.domain.interactor.deviceservice.UseCaseBindDeviceService;
import com.vfi.android.domain.interactor.deviceservice.UseCaseEmvStop;
import com.vfi.android.domain.interactor.deviceservice.UseCaseSetCTLSLedStatus;
import com.vfi.android.domain.interactor.print.PrintTaskManager;
import com.vfi.android.domain.interactor.print.UseCaseStartPrintSlip;
import com.vfi.android.domain.interactor.print.base.AbstractTransPrintTask;
import com.vfi.android.domain.interactor.repository.UseCaseGetCurTranData;
import com.vfi.android.domain.interactor.repository.UseCaseGetDeviceInfo;
import com.vfi.android.domain.interactor.repository.UseCaseGetTerminalCfg;
import com.vfi.android.domain.interactor.repository.UseCaseGetTransSwitchMap;
import com.vfi.android.domain.interactor.repository.UseCaseGetTransSwitchParameter;
import com.vfi.android.domain.interactor.repository.UseCaseSaveMenuId;
import com.vfi.android.domain.interactor.repository.UseCaseSaveMenuTitle;
import com.vfi.android.domain.interactor.transaction.UseCaseCheckAndInitTrans;
import com.vfi.android.domain.interactor.transaction.UseCaseDoNormalTrans;
import com.vfi.android.domain.interfaces.repository.IRepository;
import com.vfi.android.domain.interfaces.service.IPosService;
import com.vfi.android.domain.utils.LogUtil;
import com.vfi.android.payment.R;
import com.vfi.android.payment.presentation.mappers.MenuInfoToViewMapper;
import com.vfi.android.payment.presentation.mappers.TransInterceptorResultMapper;
import com.vfi.android.payment.presentation.mappers.MenuTitleMapper;
import com.vfi.android.payment.presentation.models.MenuViewModel;
import com.vfi.android.payment.presentation.navigation.UINavigator;
import com.vfi.android.payment.presentation.transflows.InstallmentUIFlow2;
import com.vfi.android.payment.presentation.transflows.OfflineUIFlow2;
import com.vfi.android.payment.presentation.transflows.PreAuthCompUIFlow;
import com.vfi.android.payment.presentation.transflows.PreAuthCompUIFlow2;
import com.vfi.android.payment.presentation.transflows.PreAuthUIFlow2;
import com.vfi.android.payment.presentation.transflows.SaleUIFlow2;
import com.vfi.android.payment.presentation.transflows.SettlementUIFlow;
import com.vfi.android.payment.presentation.transflows.TipAdjustUIFlow;
import com.vfi.android.payment.presentation.transflows.VoidUIFlow;
import com.vfi.android.payment.presentation.presenters.base.BasePresenter;
import com.vfi.android.payment.presentation.utils.AndroidUtil;
import com.vfi.android.payment.presentation.utils.DialogUtil;
import com.vfi.android.payment.presentation.utils.ResUtil;
import com.vfi.android.payment.presentation.utils.TransUtil;
import com.vfi.android.payment.presentation.utils.xmlparse.AppKeysConfigParser;
import com.vfi.android.payment.presentation.utils.xmlparse.XmlParser;
import com.vfi.android.payment.presentation.view.activities.history.HistoryActivity;
import com.vfi.android.payment.presentation.view.contracts.MainMenuUI;
import com.vfi.android.payment.presentation.view.menu.MenuInfo;
import com.vfi.android.payment.presentation.view.menu.MenuManager;
import com.vfi.android.payment.presentation.view.widget.LoadingDialog;
import com.vfi.smartpos.deviceservice.aidl.IDeviceService;
import com.vfi.smartpos.deviceservice.aidl.IPrinter;
import com.vfi.smartpos.deviceservice.aidl.PrinterListener;
import com.vfi.smartpos.deviceservice.aidl.QrCodeContent;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

public class MainMenuPresenter extends BasePresenter<MainMenuUI> {
    String response_logon;
    private final String TAG = TAGS.UILevel;

    private final UseCaseBindDeviceService useCaseBindDeviceService;
    private final UseCaseCheckAndInitTrans useCaseCheckAndInitTrans;
    private final UseCaseGetTerminalCfg useCaseGetTerminalCfg;
    private final UseCaseSaveMenuId useCaseSaveMenuId;
    private final UseCaseSaveMenuTitle useCaseSaveMenuTitle;
    private final UseCaseGetTransSwitchMap useCaseGetTransSwitchMap;
    private final UseCaseSetCTLSLedStatus useCaseSetCTLSLedStatus;
    private final UseCaseEmvStop useCaseEmvStop;
    private final UseCaseDoNormalTrans useCaseDoNormalTrans;
    private final MenuInfoToViewMapper menuInfoToViewMapper;
    private final MenuManager menuManager;
    private final UINavigator uiNavigator;
    private final UseCaseGetTransSwitchParameter useCaseGetTransSwitchParameter;

    private final UseCaseStartPrintSlip useCaseStartPrintSlip;
    private final DeviceInformation deviceInformation;

    private final CurrentTranData currentTranData;
    private final  IRepository repository;
    private RecordInfo recordInfo;
    @Inject
    AppKeysConfigParser appKeysConfigParser;

    @Inject
    public MainMenuPresenter(UseCaseBindDeviceService useCaseBindDeviceService,
                             MenuManager menuManager,
                             UINavigator uiNavigator,
                             UseCaseCheckAndInitTrans useCaseCheckAndInitTrans,
                             UseCaseGetTerminalCfg useCaseGetTerminalCfg,
                             UseCaseSaveMenuId useCaseSaveMenuId,
                             UseCaseSaveMenuTitle useCaseSaveMenuTitle,
                             UseCaseGetTransSwitchMap useCaseGetTransSwitchMap,
                             UseCaseSetCTLSLedStatus useCaseSetCTLSLedStatus,
                             UseCaseGetTransSwitchParameter useCaseGetTransSwitchParameter,
                             UseCaseEmvStop useCaseEmvStop,
                             UseCaseGetDeviceInfo useCaseGetDeviceInfo,
                             MenuInfoToViewMapper menuInfoToViewMapper,
                             UseCaseDoNormalTrans useCaseDoNormalTrans,
                             UseCaseStartPrintSlip useCaseStartPrintSlip,
                             UseCaseGetCurTranData useCaseGetCurTranData,
                             IRepository repository) {
        this.useCaseBindDeviceService = useCaseBindDeviceService;
        this.menuInfoToViewMapper = menuInfoToViewMapper;
        this.menuManager = menuManager;
        this.useCaseCheckAndInitTrans = useCaseCheckAndInitTrans;
        this.uiNavigator = uiNavigator;
        this.useCaseDoNormalTrans = useCaseDoNormalTrans;
        this.useCaseGetTerminalCfg = useCaseGetTerminalCfg;
        this.useCaseSaveMenuId = useCaseSaveMenuId;
        this.useCaseSaveMenuTitle = useCaseSaveMenuTitle;
        this.useCaseGetTransSwitchMap = useCaseGetTransSwitchMap;
        this.useCaseSetCTLSLedStatus = useCaseSetCTLSLedStatus;
        this.useCaseEmvStop = useCaseEmvStop;
        this.useCaseGetTransSwitchParameter = useCaseGetTransSwitchParameter;
        this.deviceInformation = useCaseGetDeviceInfo.execute(null);
        this.useCaseStartPrintSlip = useCaseStartPrintSlip;
        this.currentTranData = useCaseGetCurTranData.execute(null);
        this.recordInfo = currentTranData.getRecordInfo();
        this.repository = repository;

    }

    @Override
    protected void onFirstUIAttachment() {
        loadMenu();
        doUICmd_showTerminalSN(deviceInformation.getSerialNo());
    }

    private void loadMenu() {
        final List<MenuInfo> menuInfos = menuManager.getHomeMenuList(null);
        List<Integer> transTypeList = new ArrayList<>();
        for (MenuInfo menuInfo : menuInfos) {
            transTypeList.add(menuInfo.getTransType());
        }

        Disposable disposable = useCaseGetTransSwitchMap.asyncExecute(transTypeList).subscribe(transSwitchMap -> {
            doUICmd_showMainMenu(menuInfoToViewMapper.toViewModel(filterDisableTransMenu(menuInfos, transSwitchMap)));
        }, throwable -> {
            doUICmd_showMainMenu(menuInfoToViewMapper.toViewModel(menuInfos));
        });
    }

    private List<MenuInfo> filterDisableTransMenu(List<MenuInfo> menuInfos, Map<Integer, SwitchParameter> map) {
        if (menuInfos != null) {
            Iterator<MenuInfo> iterator = menuInfos.iterator();
            while (iterator.hasNext()) {
                MenuInfo menuInfo = iterator.next();
                SwitchParameter switchParameter = map.get(menuInfo.getTransType());
                if (switchParameter != null && !switchParameter.isEnableTrans()) {
                    iterator.remove();
                }
            }
        }
        return menuInfos;
    }

    private boolean doTransCheckAndInit(int transType) {
        boolean bRet = true;

        try {
            int ret = useCaseCheckAndInitTrans.execute(transType);
            Log.e("T_Check13",ret+"");
            if (ret != InterceptorResult.NORMAL) {
                doUICmd_showToastMessage(TransInterceptorResultMapper.toString(ret));
                bRet = false;
            }
        } catch (Exception e) {
            doUICmd_showToastMessage(ResUtil.getString(R.string.toast_hint_init_trans_exception));
            e.printStackTrace();
            bRet = false;
        }

        return bRet;
    }


    public void startTradeFlow(MenuViewModel menuViewModel) {
        if (menuViewModel.isMenuGroup()) {
            // Menu item have sub items
            useCaseSaveMenuTitle.execute(MenuTitleMapper.toString(menuViewModel.getTransType()));
            useCaseSaveMenuId.execute(menuViewModel.getMenuID());
            doUICmd_navigatorToSubMenu();
        } else {
            int selectTransType = menuViewModel.getTransType();
            LogUtil.d(TAG, "selectTransType=" + selectTransType);
            Log.d("dhaka1","dhaka1");
            if (doTransCheckAndInit(selectTransType)) {

                useCaseSaveMenuTitle.execute(MenuTitleMapper.toString(menuViewModel.getTransType()));

                TransUtil.doUINavigatorParamInit(uiNavigator, selectTransType, useCaseGetTerminalCfg, useCaseGetTransSwitchParameter);

                if(selectTransType!=TransType.LOGON){
                    XmlParser xmlParser = new XmlParser();
                    xmlParser.parserConfigFile(appKeysConfigParser);
                }

                if (selectTransType == TransType.SALE) {
                 //   SendfeedbackJob job = new SendfeedbackJob();
                 //   job.execute("sale", "sale");
                    Log.d("dataxx", "startTradeFlow: SALE");
                    uiNavigator.setTransUiFlow(new SaleUIFlow2());
                } else if (selectTransType == TransType.VOID) {
                    uiNavigator.setTransUiFlow(new VoidUIFlow());
                } else if (selectTransType == TransType.SETTLEMENT) {
                    uiNavigator.setTransUiFlow(new SettlementUIFlow());
                } else if (selectTransType == TransType.PREAUTH) {
                    uiNavigator.setTransUiFlow(new PreAuthUIFlow2());
                } else if (selectTransType == TransType.PREAUTH_COMP) {
                    uiNavigator.setTransUiFlow(new PreAuthCompUIFlow());
                } else if (selectTransType == TransType.OFFLINE) {
                    uiNavigator.setTransUiFlow(new OfflineUIFlow2());
                } else if (selectTransType == TransType.TIP_ADJUST) {
                    uiNavigator.setTransUiFlow(new TipAdjustUIFlow());
                } else if (selectTransType == TransType.INSTALLMENT) {
                    uiNavigator.setTransUiFlow(new InstallmentUIFlow2());

                } else if (selectTransType == TransType.LOGON) {

                    logon().doOnComplete(()->{
                        XmlParser xmlParser = new XmlParser();
                        recordInfo = repository.getCurrentTranData().getRecordInfo();
                        recordInfo.setMerchantIndex(1);
                        recordInfo.setSerial_number("S/N:" + deviceInformation.getSerialNo());
                        Log.e("date", recordInfo.getTransDate()+" getTransDate");
                        currentTranData.setRecordInfo(recordInfo);

                        //  repository.putCurrentTranData(currentTranData);
                        if (xmlParser.parserConfigFile(appKeysConfigParser)) {
                            doUICmd_showToastMessage("Logon Success");
                        } else {
                            doUICmd_showToastMessage("Logon Failed");
                        }
                       // progressDialog.dismiss();

                        startPrintNormalTransSlip(false);
                    }).subscribe();
//                    SendfeedbackJob job = new SendfeedbackJob();
//                    job.execute("logon", "logon");
                } else if (selectTransType == TransType.REPORT) {
                    Context Context = null;
                    AndroidUtil.startActivity(Context, HistoryActivity.class);
                }
                else if (selectTransType == TransType.CASH_ADV) {
                    uiNavigator.setTransUiFlow(new SaleUIFlow2());
                }else {

                    uiNavigator.setTransUiFlow(null);
                }

                doUICmd_navigatorToNext();
            }
        }
    }
    private Observable<Boolean> logon() {  Log.e("T_CHECK_44","Click On Logon");
        return Observable.create(e -> {
            Log.e("T_Check4","CHECK7");
            useCaseDoNormalTrans.asyncExecute(null).doOnNext(commData -> {
                LogUtil.d(TAG, "Comm status=" + commData.toString());
                Log.e("T_Check4","CHECK8");
            }).doOnComplete(() -> {
                e.onNext(true);
                e.onComplete();
                Log.e("T_Check4","CHECK9");
            }).doOnError(e::onError).subscribe();
        });


    }
    private void setPrintLogo(PrintInfo printLogo) {
        printLogo.setPrintLogoData(ResUtil.getByteFromDrawable(R.drawable.print_logo, 384f));
    }
    public void startPrintNormalTransSlip(boolean isContinueFromPrintError) {
        PrintInfo printInfo = new PrintInfo(PrintType.getPrintType(TransType.LOGON), -1);
        setPrintLogo(printInfo);
        printInfo.setContinueFromPrintError(isContinueFromPrintError);
        // doUICmd_setPrintingDialogStatus(true);
        Disposable disposable = useCaseStartPrintSlip.asyncExecute(printInfo).subscribe(unused -> {
            LogUtil.d(TAG, "currentPrintSlipType=" + printInfo.getPrintSlipType());
            //    doUICmd_setPrintingDialogStatus(false);
           /* if (checkAndGetNextPrintSlipType() < 0) {
                doUICmd_navigatorToNext();
            } else {
                doUICmd_showCountDownAskDialog(getPrintNextSlipHintMessage(), 15, new DialogUtil.AskDialogListener() {
                    @Override
                    public void onClick(boolean isSure) {
                        if (isSure) {
                            startPrintNormalTransSlip(false);
                        } else {
                            doUICmd_navigatorToNext();
                        }
                    }
                });
            }*/
        }, throwable -> {
            // doUICmd_setPrintingDialogStatus(false);
            //doPrintErrorProcess(throwable);
        });
    }
    private class SendfeedbackJob extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String[] params) {


            try {

                // Create a new socket
                String logon_requst = "0036" +
                        "6000020000" +
                        "0800" +
                        "2020010000C00004920000000019000232303030303030323437323930353030303030313030300006303030303031";


                Socket socket = new Socket();

                HostInfo hostInfo = new HostInfo();
                String hostIp = "172.31.1.42";
                int port = 5200;
                Log.d("response", hostIp + String.valueOf(port));
                socket.connect(new InetSocketAddress(hostIp, port), 30 * 1000);

                if (socket.isConnected()) {
                    Log.d("response", "connected");
                }
                // Get the input and output streams
                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream();
                // Send data to the server
                Log.d("response", "before send");
                byte[] data = hexStringToByteArray(logon_requst);
                outputStream.write(data);
                Log.d("response", "after send");
                // Receive data from the server
                Log.d("response", hexStringToByteArray(logon_requst).toString());
                Log.d("response", "before recv");
                byte[] buffer = new byte[58];
                int bytesRead = inputStream.read(buffer);
                Log.d("response", String.valueOf(bytesRead));
                String gt = byte2HexStr(buffer);
                Log.d("response1", gt);
                Log.d("response2", gt.substring((gt.length() - 32)));
                String logon_data = gt.substring(gt.length() - 32);
                String response = new String(buffer, 0, bytesRead);
                response_logon = gt.substring(gt.length() - 32);
                boolean result = sessionKeySend(logon_data);

                //startPrintNormalTransSlip(false);
                if (!params[0].equals("sale")) {
                    if (result == true) {
                        doUICmd_showToastMessage("LOGON SUCCESS");
                        Log.d("logonxx", "Logon Successful");

                    } else {
                        doUICmd_showToastMessage("LOGON FAIL");
                        Log.d("logonxx", "Logon fail");

                    }
                } else {

                }

                inputStream.close();
                outputStream.close();
                // Close the connection
                socket.close();

            } catch (Exception e) {
                if (!params[0].equals("sale")) {
                    doUICmd_showToastMessage("LOGON FAIL");
                }
                Log.d("ishtiak", e.toString());
               // startPrintNormalTransSlip(false);
            }
            return "some message";
        }


        @Override
        protected void onPostExecute(String message) {
            //process message
        }
    }

    //end
    public void openDialog() {
        Context context = null;
        final Dialog dialog = new Dialog(context); // Context, this, etc.
        dialog.setContentView(R.layout.dialog_xml);
        dialog.setTitle("LOGON");
        dialog.show();
    }

    public boolean sessionKeySend(String data) {
        String[] values = new String[10];
        try {

            String sdcardPath = Environment.getExternalStorageDirectory().getPath();
            try {

                File xmlFile = new File(sdcardPath + "/demo_xml/App_Keys.xml");

                FileInputStream fis = new FileInputStream(xmlFile);
                XmlPullParser parser = Xml.newPullParser();

                parser.setInput(fis, null);

                String tagName = null;
                int i = 0;
                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        String tagName1 = parser.getName();
                        Log.d("valxx", "nanme " + tagName1);
                        // Do something with the tag name
                    } else if (eventType == XmlPullParser.TEXT) {
                        String text = parser.getText();

                        if (text.length() == 32) {
                            values[i] = text;
                            Log.d("valxxxx" + i, "value " + text);
                            i++;
                        }

                    }
                    eventType = parser.next();
                }


                // Close the input stream
                fis.close();
            } catch (Exception e) {
                Log.d("ishtiakxxx", e.toString());
                e.printStackTrace();
            }
            Log.d("pinxxx", values[0]);
            Log.d("pinxxx", values[1]);
            String xmlString = "<?xml version=\"1.1\" encoding=\"UTF-8\"?>\n" +
                    "<APP_KEYS>\n" +
                    "\t <TEK>" + values[0] + "</TEK>\n" +
                    "\t<TMK keyIndex=\"0\">" + values[1] + "</TMK> \n" +
                    "\t<PIN_KEY tmkIndex=\"0\" keyIndex=\"0\" kcv=\"\">" +
                    data
                    +
                    "</PIN_KEY>" +
                    "</APP_KEYS>";

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource inputSource = new InputSource(new StringReader(xmlString));
            Document document = builder.parse(inputSource);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(new File(sdcardPath + "/demo_xml/App_Keys.xml"));
            transformer.transform(source, result);

            XmlParser xmlParser = new XmlParser();
            if (xmlParser.parserConfigFile(appKeysConfigParser)) {
                return true;
            } else {
                return false;
            }


        } catch (Exception e) {
            return false;
        }


    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String byte2HexStr(byte[] data) {
        if (data == null || data.length <= 0) {
            return null;
        }
        try {
            String stmp = "";
            StringBuilder sb = new StringBuilder("");
            for (int n = 0; n < data.length; n++) {
                stmp = Integer.toHexString(data[n] & 0xFF);
                sb.append(stmp.length() == 1 ? "0" + stmp : stmp);
            }
            return sb.toString().toUpperCase().trim();
        } catch (Exception e) {
        }
        return null;
    }

    public void updateIsDoingTrans(boolean isDoingTrans) {

    }

    public void checkBindServiceStatus() {
        useCaseBindDeviceService.asyncExecuteWithoutResult(null);
    }

    public void checkPowerStatus(Context context) {

    }

    public void checkTmsParameters() {

    }

    public void checkCTLSLedStatus() {
        useCaseSetCTLSLedStatus.asyncExecuteWithoutResult(CTLSLedStatus.CLEAR_ALL_LEDS);
    }

    public void checkStopEmv() {
        useCaseEmvStop.asyncExecuteWithoutResult(null);
    }

    private void doUICmd_showMainMenu(ArrayList<MenuViewModel> menuModels) {
        execute(ui -> ui.showMainMenu(menuModels));
    }

    private void doUICmd_navigatorToSubMenu() {
        execute(ui -> ui.navigatorToSubMenu());
    }

    private void doUICmd_showTerminalSN(String sn) {
        execute(ui -> ui.showTerminalSN(sn));
    }
}
