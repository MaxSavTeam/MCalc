package com.maxsavteam.newmcalc2.ui;import android.annotation.SuppressLint;import android.content.Intent;import android.content.pm.ActivityInfo;import android.os.Bundle;import android.util.TypedValue;import android.view.Gravity;import android.view.MenuItem;import android.view.animation.AnimationUtils;import android.widget.EditText;import android.widget.TextSwitcher;import android.widget.TextView;import android.widget.Toast;import androidx.appcompat.widget.Toolbar;import com.maxsavteam.newmcalc2.Main2Activity;import com.maxsavteam.newmcalc2.R;import com.maxsavteam.newmcalc2.ThemeActivity;import com.maxsavteam.newmcalc2.utils.Utils;import java.math.BigInteger;import java.util.Locale;import java.util.Random;public class NumberGeneratorActivity extends ThemeActivity {	private final Random mRandom = new Random();	@SuppressLint("SourceLockedOrientationActivity")	@Override	protected void onCreate(Bundle savedInstanceState) {		super.onCreate( savedInstanceState );		setContentView( R.layout.activity_numgen );		Toolbar toolbar = findViewById( R.id.toolbar );		setSupportActionBar( toolbar );		getSupportActionBar().setDisplayHomeAsUpEnabled( true );		setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_PORTRAIT );		TextSwitcher textSwitcher = findViewById( R.id.textSwitcherNumGen );		textSwitcher.setFactory( ()->{			TextView textView = new TextView( this );			textView.setTextColor( super.textColor );			textView.setTextSize( TypedValue.COMPLEX_UNIT_SP, 30 );			textView.setGravity( Gravity.CENTER );			textView.setTextIsSelectable( true );			return textView;		} );		textSwitcher.setInAnimation( AnimationUtils.loadAnimation( this, android.R.anim.fade_in ) );		textSwitcher.setOutAnimation( AnimationUtils.loadAnimation( this, android.R.anim.fade_out ) );		findViewById( R.id.btnGen ).setOnClickListener( v -> {			EditText minInput = findViewById( R.id.edTextMin );			EditText maxInput = findViewById( R.id.edTextMax );			String maxs = maxInput.getText().toString();			String mins = minInput.getText().toString();			if ( !minInput.getText().toString().equals( "" ) && !maxInput.getText().toString().equals( "" ) ) {				BigInteger b1 = new BigInteger( mins );				BigInteger b2 = new BigInteger( maxs ), lmax = BigInteger.valueOf( Long.MAX_VALUE ), lmin = BigInteger.valueOf( Long.MIN_VALUE );				if ( b1.compareTo( lmin ) < 0 || b1.compareTo( lmax ) > 0 || b2.compareTo( lmin ) < 0 || b2.compareTo( lmax ) > 0 ) {					Toast.makeText( this, getString( R.string.value_is_too_big ), Toast.LENGTH_LONG ).show();					return;				}				int minNum = Integer.parseInt( minInput.getText().toString() );				int maxNum = Integer.parseInt( maxInput.getText().toString() );				if ( minNum > maxNum ) {					int temp = minNum;					minNum = maxNum;					maxNum = temp;				}				int randomNum = minNum + mRandom.nextInt( maxNum - minNum + 1 );				((TextSwitcher) findViewById( R.id.textSwitcherNumGen )).setText( String.format( Locale.ROOT, "%d", randomNum ) );			}		} );	}	@Override	public boolean onOptionsItemSelected(MenuItem item) {		int id = item.getItemId();		if ( id == android.R.id.home ) {			onBackPressed();		}		return super.onOptionsItemSelected( item );	}	@Override	public void onBackPressed() {		if ( "shortcut".equals( getIntent().getStringExtra( "type" ) ) ) {			startActivity( new Intent( this, Main2Activity.class ) );		}		super.onBackPressed();		Utils.defaultActivityAnim( this );	}}