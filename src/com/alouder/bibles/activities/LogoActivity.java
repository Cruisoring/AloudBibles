package com.alouder.bibles.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;

import com.alouder.bibles.R;
import com.alouder.bibles.data.BiblesContentProvider;

public class LogoActivity extends Activity {
	private static final int SPLASH_DISPLAY_TIME = 300; // splash screen delay time
	
	Runnable theRunnable = new Runnable() {
			
			@Override
			public void run() {
	            Intent intent = new Intent();
	            intent.setClass(LogoActivity.this, BiblesContentProvider.primaryWork == null ?
	            		DownloadActivity.class : BooksActivity.class);

	            LogoActivity.this.startActivity(intent);
	            LogoActivity.this.finish();
				
			}
		};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.logo);
		ImageView imageLogo = (ImageView) findViewById(R.id.imageLogo);
		AlphaAnimation animation = new AlphaAnimation(0.0f , 1.0f ) ;
		
		final Handler mHandler = new Handler();
		mHandler.postDelayed(theRunnable, SPLASH_DISPLAY_TIME);
		
		imageLogo.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mHandler.removeCallbacks(theRunnable);
				Intent intent = new Intent();
				intent.setClass(LogoActivity.this, BooksActivity.class);
				
				LogoActivity.this.startActivity(intent);
				LogoActivity.this.finish();
			}
		});
		animation.setFillAfter(true);
		animation.setDuration(1000);
		//apply the animation ( fade In ) to your LAyout
		imageLogo.startAnimation(animation);
	}


}
