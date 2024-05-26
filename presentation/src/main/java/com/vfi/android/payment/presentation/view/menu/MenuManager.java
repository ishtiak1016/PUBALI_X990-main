package com.vfi.android.payment.presentation.view.menu;

import com.vfi.android.domain.entities.consts.TransType;
import com.vfi.android.domain.entities.databeans.MenuOrder;
import com.vfi.android.payment.R;
import com.vfi.android.payment.presentation.utils.ResUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MenuManager {
    private TreeMap<Integer, MenuInfo> menuInfoHashMap;

    @Inject
    public MenuManager() {
        menuInfoHashMap = new TreeMap<>();
        MenuInfo menuInfo;

        menuInfo = new MenuInfo(R.string.menu_title_sale, R.drawable.icon_sale_aamra, TransType.SALE, 1, -1, false, true);
        menuInfoHashMap.put(menuInfo.getMenuId(), menuInfo);
        menuInfo = new MenuInfo(R.string.menu_name_settlement, R.drawable.icon_settlement_aamra, TransType.SETTLEMENT, 2, -1, false, true);
        menuInfoHashMap.put(menuInfo.getMenuId(), menuInfo);
        menuInfo = new MenuInfo(R.string.menu_name_void, R.drawable.icon_reverse_aamra, TransType.VOID, 3, -1, false, true);
        menuInfoHashMap.put(menuInfo.getMenuId(), menuInfo);
        menuInfo = new MenuInfo(R.string.menu_name_preauth, R.drawable.icon_preauth_aamra, TransType.PREAUTH, 4, -1, false, true);
        menuInfoHashMap.put(menuInfo.getMenuId(), menuInfo);
        menuInfo = new MenuInfo(R.string.menu_name_preauth_comp, R.drawable.icon_precom_aamra, TransType.PREAUTH_COMP, 5, -1, false, true);
        menuInfoHashMap.put(menuInfo.getMenuId(), menuInfo);
        menuInfo = new MenuInfo(R.string.menu_name_offline_sale, R.drawable.icon_offline_sale_aamra, TransType.OFFLINE, 6, -1, false, false);
        menuInfoHashMap.put(menuInfo.getMenuId(), menuInfo);
        menuInfo = new MenuInfo(R.string.menu_name_tip_adjust, R.drawable.icon_tip_aamra, TransType.TIP_ADJUST, 7, -1, false, true);
        menuInfoHashMap.put(menuInfo.getMenuId(), menuInfo);
        menuInfo = new MenuInfo(R.string.menu_name_installment, R.drawable.icon_emi_sale_aamra, TransType.INSTALLMENT, 8, -1, false, true);
        menuInfoHashMap.put(menuInfo.getMenuId(), menuInfo);
        menuInfo = new MenuInfo(R.string.menu_title_logon, R.drawable.icon_logon_aamra, TransType.LOGON, 9, -1, false, true);
        menuInfoHashMap.put(menuInfo.getMenuId(), menuInfo);

        menuInfo = new MenuInfo(R.string.title_cash_advance, R.drawable.icon_preauth_aamra, TransType.CASH_ADV, 10, -1, false, true);
        menuInfoHashMap.put(menuInfo.getMenuId(), menuInfo);

    }

    public List<MenuInfo> getHomeMenuList(List<MenuOrder> menuOrders) {
        return getDefaultOrderHomeMenuInfoList();
    }

    public List<MenuInfo> getSubMenuList(int parentMenuId) {
        List<MenuInfo> menuInfos = new ArrayList<>();

        Iterator iterator = menuInfoHashMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, MenuInfo> entry = (Map.Entry<Integer, MenuInfo>) iterator.next();
            MenuInfo menuInfo = entry.getValue();
            if (menuInfo.getParentMenuId() == parentMenuId) {
                menuInfos.add(menuInfo);
            }
        }

        return menuInfos;
    }

    private List<MenuInfo> getDefaultOrderHomeMenuInfoList() {
        List<MenuInfo> menuInfos = new ArrayList<>();
        Iterator iterator = menuInfoHashMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, MenuInfo> entry = (Map.Entry<Integer, MenuInfo>) iterator.next();
            if (entry.getValue().isShowOnMainMenu()) {
                menuInfos.add(entry.getValue());
            }
        }

        return menuInfos;
    }
}
