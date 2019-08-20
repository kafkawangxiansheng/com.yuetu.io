package org.spring.boot.multiple.ds.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spring.boot.multiple.ds.TargetDataSource;
import org.spring.boot.multiple.ds.bean.DateInfo;
import org.spring.boot.multiple.ds.bean.Tvoucher;
import org.spring.boot.multiple.ds.bean.TvoucherEntry;
import org.spring.boot.multiple.ds.bean.YmlProp;
import org.spring.boot.multiple.ds.dao.MerchandiseShiftBSDao;
import org.spring.boot.multiple.ds.dao.MerchandiseShiftKingDeeDao;
import org.spring.boot.multiple.ds.service.MerchandiseShiftService;
import org.spring.boot.multiple.ds.util.DateUtil;
import org.spring.boot.multiple.ds.util.VoucherUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author 刘世杰
 * @date 2019/8/19
 * 商品移仓单impl
 */
@Service
public class MerchandiseShiftServiceImpl implements MerchandiseShiftService {

    @Autowired
    MerchandiseShiftBSDao merchandiseShiftBSDao;

    @Autowired
    MerchandiseShiftKingDeeDao merchandiseShiftKingDeeDao;

    @Autowired
    YmlProp ymlProp;

    /**
     * SLF4J
     */
    Logger log = LoggerFactory.getLogger(getClass());

    @TargetDataSource("dataSource")
    @Override
    public List<Tvoucher> merchandiseShiftVoucher(DateInfo date) {
        log.info("进入merchandiseShiftVoucher方法,查询百盛商品移仓单数据处理并生成单据集合");
        List<Object> list = merchandiseShiftBSDao.getVoucher(date);
        DateInfo dateInfo = DateUtil.dateData();
        List<Tvoucher> tVoucherList = new ArrayList<>();
        for(Object obj : list) {
            if(!ObjectUtils.isEmpty(obj)) {
                Map<String,Object> map = (Map<String, Object>)obj;
                Tvoucher voucher = VoucherUtil.getVoucher();
                //凭证日期
                voucher.setFDate(dateInfo.getCurrentMonth25());
                //业务日期
                voucher.setFTransDate(dateInfo.getCurrentMonth25());
                //年份
                voucher.setFYear(dateInfo.getCurrentYear());
                //会计期间
                voucher.setFPeriod(dateInfo.getCurrentMonth());
                //摘要
                voucher.setFExplanation("");
                //分录数
                voucher.setFEntryCount(String.valueOf(map.get("TOTAL")));
                //借方金额合计
                voucher.setFDebitTotal(String.valueOf(map.get("CBJE")));
                //贷方金额合计
                voucher.setFCreditTotal(String.valueOf(map.get("CBJE")));
                //贷方:CKMC1
                voucher.setCredit(String.valueOf(map.get("CKMC1")));
                //返回百盛获取到的数据
                tVoucherList.add(voucher);
            }
        }
        return tVoucherList;
    }

    @TargetDataSource("dataSource")
    @Override
    public List<TvoucherEntry> merchandiseShiftVoucherEntry(DateInfo date) {
        log.info("进入merchandiseShiftVoucherEntry方法,查询百盛商品移仓单数据处理并生成单据分录集合");
        List<Object> list = merchandiseShiftBSDao.getVoucherEntry(date);
        List<TvoucherEntry> tVoucherEntryList = new ArrayList<>();
        for(Object obj : list) {
            if(!ObjectUtils.isEmpty(obj)) {
                TvoucherEntry tvoucherEntry = VoucherUtil.getVoucherEntry();
                Map<String, Object> map = (Map<String, Object>) obj;
                //摘要
                tvoucherEntry.setFExplanation("");
                //余额方向 0-贷方,1- 借方
                tvoucherEntry.setFDC("1");
                //金额（原币）
                tvoucherEntry.setFAmountFor(String.valueOf(map.get("CBJE")));
                //金额(本位币)
                tvoucherEntry.setFAmount(String.valueOf(map.get("CBJE")));
                //贷方:CKMC1
                tvoucherEntry.setCredit((String)map.get("CKMC1"));
                //借方:CKMC
                tvoucherEntry.setDebit((String)map.get("CKMC"));
                tVoucherEntryList.add(tvoucherEntry);
            }
        }
        return tVoucherEntryList;
    }

    @TargetDataSource("ds1")
    @Override
    public void insertMerchandiseShiftVoucher(List<Tvoucher> tVoucherList, List<TvoucherEntry> tVoucherEntryList) {
        log.info("进入insertMerchandiseShiftVoucher方法,插入商品调仓单数据到金蝶凭证");
        for(Tvoucher tvoucher  : tVoucherList) {
            Tvoucher obj = merchandiseShiftKingDeeDao.selectLastVoucher();
            String FVoucherID = "0";
            if(obj != null) {
                FVoucherID = String.valueOf(Integer.parseInt(obj.getFVoucherID())+1);
            }
            tvoucher.setFVoucherID(FVoucherID);
            tvoucher.setFNumber(FVoucherID);
            tvoucher.setFSerialNum(FVoucherID);
            //插入凭证数据到金蝶系统
            merchandiseShiftKingDeeDao.addVoucher(tvoucher);
        }
    }
}
