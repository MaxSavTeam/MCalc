package com.maxsavteam.newmcalc2.core;

import android.content.Context;
import android.util.Log;

import java.math.BigDecimal;

public class CoreSubProcess {

	private BigDecimal mResult = null;
	private Context mContext;
	private String TAG = "CoreSubProcess";

	public CalculationError getError() {
		return error;
	}

	public boolean isWasError() {
		return mWasError;
	}

	private CalculationError error;

	private boolean mWasError = false;
	public BigDecimal getResult() {
		return mResult;
	}

	CalculationCore.CoreLinkBridge mCoreLinkBridge = new CalculationCore.CoreLinkBridge() {
		@Override
		public void onSuccess(CalculationCore.CalculationResult calculationResult) {
			mResult = calculationResult.getResult();
		}

		@Override
		public void onError(CalculationError error) {
			mWasError = true;
			CoreSubProcess.this.error = error;
			mResult = null;
			if(error.getStatus().equals("Core")) {
				if (error.getErrorMessage().contains("String is number")) {
					mResult = error.getPossibleResult();
				}
			}
		}
	};

	public void run(String ex) {
		CalculationCore calculationCore = new CalculationCore(mContext);
		calculationCore.setInterface(mCoreLinkBridge);

		Log.v( TAG, "run with ex=" + ex );
		calculationCore.prepareAndRun(ex, "isolated");
	}

	CoreSubProcess(Context context){
		this.mContext = context;
		Log.v( TAG, "constructor" );
	}

}