package com.healthmanage.dao;

import java.util.Map;

import com.healthmanage.config.EnvConfig;
import com.healthmanage.model.Coupon;
import com.healthmanage.model.Gym;
import com.healthmanage.utils.FileIO;

public class CouponDAO {
	private static CouponDAO instance;
	private CouponDAO() {
		
	}
	public static CouponDAO getInstance() {
		if (instance == null) {
			instance = new CouponDAO();
		}
		return instance;
	}
	public void saveCoupons() {
		FileIO.infoSave(Gym.coupons, EnvConfig.get("COUPON_FILE"));
	}

	public Map<String, Coupon> loadCoupons(String FilePath) {
		Gym.coupons = (Map<String, Coupon>) FileIO.infoLoad(FilePath,Coupon.class);
		return Gym.coupons;
	}
}
