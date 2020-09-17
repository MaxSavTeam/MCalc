package com.maxsavteam.newmcalc2.ui;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.maxsavteam.newmcalc2.BuildConfig;
import com.maxsavteam.newmcalc2.Main2Activity;
import com.maxsavteam.newmcalc2.R;
import com.maxsavteam.newmcalc2.ThemeActivity;
import com.maxsavteam.newmcalc2.adapters.MyRecyclerViewAdapter;
import com.maxsavteam.newmcalc2.adapters.MyRecyclerViewAdapter.ViewHolder;
import com.maxsavteam.newmcalc2.swipes.SwipeController;
import com.maxsavteam.newmcalc2.swipes.SwipeControllerActions;
import com.maxsavteam.newmcalc2.types.HistoryEntry;
import com.maxsavteam.newmcalc2.utils.Constants;
import com.maxsavteam.newmcalc2.utils.HistoryStorageProtocolsFormatter;
import com.maxsavteam.newmcalc2.utils.ResultCodes;
import com.maxsavteam.newmcalc2.utils.Utils;
import com.maxsavteam.newmcalc2.widget.CustomAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

public class HistoryActivity extends ThemeActivity implements MyRecyclerViewAdapter.ItemClickListener {

	private MyRecyclerViewAdapter adapter;

	private SharedPreferences sp;
	private Intent history_action;
	private ArrayList<HistoryEntry> str = new ArrayList<>();
	private RecyclerView rv;
	private boolean needToCreateMenu = false;
	private Menu mMenu;
	private int LOCAL_HISTORY_STORAGE_PROTOCOL_VERSION;

	private void backPressed() {
		//sendBroadcast(history_action);
		finish();
		overridePendingTransition( R.anim.activity_in1, R.anim.activity_out1 );
	}

	private void restartActivity() {
		Intent in = new Intent( this, HistoryActivity.class );
		this.startActivity( in );
		this.finish();
	}

	@Override
	public void onBackPressed() {
		setResult( ResultCodes.RESULT_NORMAL, history_action );
		if ( start_type.equals( "app" ) ) {
			backPressed();
		} else if ( start_type.equals( "shortcut" ) ) {
			startActivity( new Intent( this, Main2Activity.class ) );
			backPressed();
		}
		//super.onBackPressed();
	}

	private void onSomethingWentWrong() {
		Toast.makeText( this, getResources().getString( R.string.smth_went_wrong ), Toast.LENGTH_LONG ).show();
		history_action.putExtra( "error", true );
		setResult( ResultCodes.RESULT_ERROR, history_action );
		backPressed();
	}

	@Override
	public void onItemClick(View view, int position) {
		if ( position == POSITION_TO_DEL ) {
			cancelTimer();
			animate_hide();
		}
		//history_action = new Intent(BuildConfig.APPLICATION_ID + ".HISTORY_ACTION");
		history_action.putExtra( "example", str.get( position ).getExample() ).putExtra( "result", adapter.getItem( position ).getAnswer() );
		setResult( ResultCodes.RESULT_NORMAL, history_action );
		backPressed();
		//Toast.makeText(this, "You clicked " + adapter.getItem(position).get(0) + " " + adapter.getItem(position).get(0) + " on row number " + position, Toast.LENGTH_SHORT).show();
	}

	int POSITION_TO_DEL = -1;
	View VIEW_ON_DELETE;

	@Override
	public void onDelete(int position, ViewHolder v) {
		try {
			if ( POSITION_TO_DEL == -1 && IN_ORDER ) {
				Toast.makeText( this, "Something went wrong. Please, restart activity", Toast.LENGTH_SHORT ).show();
				return;
			}

			if ( IN_ORDER ) {
				return;
			}
			View view = v.itemView;
			view.setBackgroundTintList( ColorStateList.valueOf( Color.RED ) );
			TextView t;
			for (int id : new int[]{ R.id.tvAns, R.id.tvAns2 }) {
				t = view.findViewById( id );
				t.setTextColor( getResources().getColor( R.color.white ) );
			}
			//view.setEnabled(false);
			VIEW_ON_DELETE = view;
			POSITION_TO_DEL = position;
			showDeleteCountdownAndRun();
		} catch (Exception e) {
			Toast.makeText( this, e.toString() + "\n\n" + position + "\n\n" + rv.getLayoutManager().getChildCount(), Toast.LENGTH_LONG ).show();
			cancelTimer();
			animate_hide();
		}
	}

	private final int SECONDS_BEFORE_DELETE = 5;

	@SuppressLint("DefaultLocale")
	private void setupTimer() {
		if ( mTimer != null ) {
			mTimer.cancel();
		}
		countdown_del = SECONDS_BEFORE_DELETE;
		mTimer = new Timer();
		cancel = findViewById( R.id.cancel_delete );
		tCount = cancel.findViewById( R.id.txtCountDown );
		tCount.setText( String.format( "%d", countdown_del ) );
		MyTimer myTimer = new MyTimer();
		mTimer.schedule( myTimer, 600, 1000 );
	}

	View.OnLongClickListener forceDelete = new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			delete();
			animate_hide();
			cancelTimer();
			setupRecyclerView();
			return true;
		}
	};

	private void cancelTimer() {
		IN_ORDER = false;
		if ( mTimer != null ) {
			mTimer.cancel();
			mTimer = null;
		}
	}

	@SuppressLint("DefaultLocale")
	private void showDeleteCountdownAndRun() {
		LinearLayout lay = findViewById( R.id.cancel_delete );
		lay.setVisibility( View.VISIBLE );
		( (TextView) lay.findViewById( R.id.txtCountDown ) ).setText( String.format( "%d", SECONDS_BEFORE_DELETE ) );
		Animation animation = AnimationUtils.loadAnimation( getApplicationContext(), R.anim.anim_scale_show );
		//animation.setDuration(500);
		animation.setAnimationListener( new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {

			}

			@SuppressLint("SetTextI18n")
			@Override
			public void onAnimationEnd(Animation animation) {
				//setupTimer();
				if ( !sp.getBoolean( "force_delete_guide_was_showed", false ) ) {
					findViewById( R.id.btnCancel ).setEnabled( false );
					new MaterialTapTargetPrompt.Builder( HistoryActivity.this )
							.setTarget( R.id.btnCancel )
							.setPrimaryText( getResources().getString( R.string.force_delete ) )
							.setSecondaryText( getResources().getString( R.string.force_delete_guide_text ) )
							.setPromptStateChangeListener( new MaterialTapTargetPrompt.PromptStateChangeListener() {
								@Override
								public void onPromptStateChanged(@NonNull MaterialTapTargetPrompt prompt, int state) {
									if ( state == MaterialTapTargetPrompt.STATE_DISMISSED || state == MaterialTapTargetPrompt.STATE_FINISHED ) {
										sp.edit().putBoolean( "force_delete_guide_was_showed", true ).apply();
										setupTimer();
										findViewById( R.id.btnCancel ).setEnabled( true );
									}
								}
							} ).show();
				} else {
					setupTimer();
				}
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		} );
		lay.setAnimation( animation );
		lay.animate();
		animation.start();
	}

	@Override
	public void showInfoButtonPressed(ViewHolder viewHolder, int position) {
		//view = (View) view.getParent().getParent();
		View view = viewHolder.itemView;
		TextView t = view.findViewById( R.id.tvWithDesc );
		boolean with = Boolean.parseBoolean( t.getText().toString() );
		if ( view.findViewById( R.id.with_desc ).getVisibility() == View.VISIBLE
				|| view.findViewById( R.id.without_desc ).getVisibility() == View.VISIBLE ) {
			LinearLayout lay;
			if ( with ) {
				lay = view.findViewById( R.id.with_desc );
			} else {
				lay = view.findViewById( R.id.without_desc );
			}
			Animation animation = AnimationUtils.loadAnimation( getApplicationContext(), R.anim.anim_scalefordesc_hide );
			animation.setAnimationListener( new Animation.AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					lay.setVisibility( View.GONE );
				}

				@Override
				public void onAnimationRepeat(Animation animation) {

				}
			} );
			lay.clearAnimation();
			lay.setAnimation( animation );
			lay.animate();
			animation.start();
		} else {
			LinearLayout lay;
			if ( with ) {
				lay = view.findViewById( R.id.with_desc );
			} else {
				lay = view.findViewById( R.id.without_desc );
			}
			Animation anim = AnimationUtils.loadAnimation( getApplicationContext(), R.anim.anim_scalefordesc_show );
			lay.clearAnimation();
			anim.setAnimationListener( new Animation.AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					//lay.setVisibility(View.VISIBLE);
					lay.clearAnimation();
				}

				@Override
				public void onAnimationRepeat(Animation animation) {

				}
			} );
			lay.setAnimation( anim );
			lay.setVisibility( View.VISIBLE );
			lay.animate();
			anim.start();
		}
	}

	@Override
	public void onDescriptionDelete(View view, int position) {
		AlertDialog alertDialog = new CustomAlertDialogBuilder( this )
				.setCancelable( false )
				.setNegativeButton( R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						dialogInterface.cancel();
					}
				} )
				.setPositiveButton( R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						str.get( position ).setDescription( null );

						LinearLayout lay = view.findViewById( R.id.with_desc );
						Animation animation = AnimationUtils.loadAnimation( getApplicationContext(), R.anim.anim_scalefordesc_hide );
						animation.setAnimationListener( new Animation.AnimationListener() {
							@Override
							public void onAnimationStart(Animation animation) {

							}

							@Override
							public void onAnimationEnd(Animation animation) {
								lay.setVisibility( View.GONE );
								adapter.clearViews(); adapter.notifyDataSetChanged();
							}

							@Override
							public void onAnimationRepeat(Animation animation) {

							}
						} );
						lay.clearAnimation();
						lay.setAnimation( animation );
						lay.animate();
						animation.start();
						//par.findViewById(R.id.with_desc).setVisibility(View.GONE);
						//par.findViewById(R.id.without_desc).setVisibility(View.VISIBLE);
						saveHistory();
					}
				} )
				.setTitle( R.string.confirm )
				.setMessage( R.string.confirm_del_desc )
				.create();
		alertDialog.show();
	}

	@Override
	public void onEditAdd(View view, int position, String mode) {
		final EditText input = new EditText( this );
		input.setBackgroundTintList( ColorStateList.valueOf( getResources().getColor( R.color.colorAccent ) ) );
		input.setTextColor( super.textColor );
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT );
		input.setLayoutParams( lp );
		AlertDialog al = new CustomAlertDialogBuilder( this )
				.setCancelable( false )
				.setView( input )
				.setMessage( R.string.enter_text )
				.setTitle( R.string.desc )
				.setPositiveButton( "OK", (dialogInterface, i)->{
					String newDesc = input.getText().toString();
					if ( mode.equals( "edit" ) ) {
						newDesc = Utils.trim( newDesc );
						if ( !newDesc.equals( "" ) ) {
							str.get( position ).setDescription( newDesc );
							saveHistory();
							( (TextView) view.findViewById( R.id.txtDesc2 ) ).setText( newDesc );
						} else {
							str.get( position ).setDescription( null );
							saveHistory();
							view.findViewById( R.id.with_desc ).setVisibility( View.GONE );
							view.findViewById( R.id.without_desc ).setVisibility( View.VISIBLE );
						}
						adapter.clearViews(); adapter.notifyDataSetChanged();
					} else if ( mode.equals( "add" ) ) {
						newDesc = Utils.trim( newDesc );
						if ( newDesc.equals( "" ) ) {
							return;
						}

						str.get( position ).setDescription( newDesc );

						view.findViewById( R.id.without_desc ).setVisibility( View.GONE );
						( (TextView) view.findViewById( R.id.txtDesc2 ) ).setText( newDesc );
						view.findViewById( R.id.with_desc ).setVisibility( View.GONE );
						saveHistory();
					}
					adapter.clearViews(); adapter.notifyDataSetChanged();
					dialogInterface.cancel();
				} )
				.setNegativeButton( R.string.cancel, (dialog, i)->dialog.cancel() )
				.create();
		if ( mode.equals( "edit" ) ) {
			String desc = ( (TextView) view.findViewById( R.id.txtDesc2 ) ).getText().toString();
			input.append( desc );
		}
		al.show();
	}

	@Override
	public void onTrimmedDescription(String newDescription, int position) {
		str.get( position ).setDescription( newDescription );
	}

	private void saveHistory() {
		StringBuilder save = new StringBuilder();
		int len = str.size();
		for (int i = 0; i < len; i++) {
			save.append( str.get( i ).getExample() );
			if ( str.get( i ).getDescription() != null ) {
				save.append( Constants.HISTORY_DESC_SEPARATOR ).append( str.get( i ).getDescription() );
			}
			save.append( ( (char) 30 ) ).append( str.get( i ).getAnswer() ).append( ( (char) 29 ) );
		}
		sp.edit().putString( "history", save.toString() ).apply();

	}

	private void delete() {
		cancelTimer();
		str.remove( POSITION_TO_DEL );
		saveHistory();
		setupRecyclerView();
	}

	private void setupRecyclerView() {
		if ( str.size() == 0 ) {
			needToCreateMenu = false;
			if ( mMenu != null ) {
				mMenu.removeItem( R.id.clear_history );
				invalidateOptionsMenu();
			}
			setContentView( R.layout.history_not_found );
			Toolbar toolbar = findViewById( R.id.toolbar );
			toolbar.setTitle( "" );
			setSupportActionBar( toolbar );
			getSupportActionBar().setDisplayHomeAsUpEnabled( true );
			TextView t = findViewById( R.id.txtHistoryNotFound );
			String[] strings = getResources().getStringArray( R.array.history_not_found );
			t.setText( String.format( "%s\n%s\n%s", strings[ 0 ], strings[ 1 ], strings[ 2 ] ) );
		} else {
			try {
				LinearLayoutManager lay = new LinearLayoutManager( this );
				lay.setOrientation( RecyclerView.VERTICAL );
				adapter = new MyRecyclerViewAdapter( getApplicationContext(), str );
				adapter.setClickListener( this );
				rv.setAdapter( adapter );
				rv.setLayoutManager( lay );
				SwipeController sc = new SwipeController( new SwipeControllerActions() {
					@Override
					public void onRightClicked(int position) {
						ArrayList<MyRecyclerViewAdapter.ViewHolder> ar = adapter.getViews();
						/*
						 * Crutch, because recyclerview returns child count one value, but adapter returns right value.
						 * I tried to google, but so far I haven't found anything.
						 */
						onDelete( position, ar.get( position ) );
						//Toast.makeText(history.this, Integer.toString(adapter.getViews().size()), Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onLeftClicked(int position) {
						showInfoButtonPressed( adapter.getViews().get( position ), position );
					}
				}, this );
				ItemTouchHelper itemTouchHelper = new ItemTouchHelper( sc );
				itemTouchHelper.attachToRecyclerView( rv );
				rv.addItemDecoration( new RecyclerView.ItemDecoration() {
					@Override
					public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
						sc.onDraw( c );
					}
				} );
			} catch (Exception e) {
				Toast.makeText( getApplicationContext(), e.toString(), Toast.LENGTH_LONG ).show();
				Log.e( "Layout", e.toString() );
			}
		}
	}

	public void cancel(View v) {
		cancelTimer();
		IN_ORDER = false;
		POSITION_TO_DEL = -1;
		VIEW_ON_DELETE.setEnabled( true );
		animate_hide();
	}

	Timer mTimer;
	int countdown_del = 6;
	TextView tCount;
	LinearLayout cancel;
	public boolean IN_ORDER = false;

	class MyTimer extends TimerTask {
		@SuppressLint("SetTextI18n")
		@Override
		public void run() {
			if ( countdown_del != 0 ) {
				countdown_del--;
				IN_ORDER = true;
				runOnUiThread( ()->tCount.setText( Integer.toString( countdown_del ) ) );

			} else {
				cancelTimer();
				IN_ORDER = false;
				runOnUiThread( HistoryActivity.this::delete );
				animate_hide();
			}
		}
	}

	public void animate_hide() {
		Animation anim = AnimationUtils.loadAnimation( getApplicationContext(), R.anim.anim_scale_hide );
		try {
			cancel.clearAnimation();
			cancel.setAnimation( anim );
			anim.setAnimationListener( new Animation.AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					cancel.setVisibility( View.GONE );
				}

				@Override
				public void onAnimationRepeat(Animation animation) {

				}
			} );
			cancel.animate();
			anim.start();
		} catch (Exception e) {
			onSomethingWentWrong();
		}
	}

	String start_type;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate( R.menu.menu_history, menu );
		mMenu = menu;
		if ( !needToCreateMenu ) {
			mMenu.removeItem( R.id.clear_history );
		}
		return super.onCreateOptionsMenu( menu );
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		//Toast.makeText(getApplicationContext(), Integer.toString(id) + " " + Integer.toString(R.id.home), Toast.LENGTH_SHORT).show();
		if ( id == android.R.id.home ) {
			backPressed();
		} else if ( id == R.id.clear_history ) {
			CustomAlertDialogBuilder build = new CustomAlertDialogBuilder( this );
			build.setMessage( R.string.confirm_cls_history )
					.setCancelable( false )
					.setPositiveButton( R.string.yes, (dialog, which)->{
						dialog.cancel();
						sp.edit().remove( "history" ).apply();
						str = new ArrayList<>();
						setupRecyclerView();
					} ).setNegativeButton( R.string.no, (dialog, which)->dialog.cancel() );
			build.create().show();
		}
		return super.onOptionsItemSelected( item );
	}

	private void prepareHistoryForRecyclerView() {
		String his = sp.getString( "history", null );
		//reformatHistory();
		if ( his != null ) {
			ProgressDialog progressDialog = new ProgressDialog( this );
			progressDialog.requestWindowFeature( Window.FEATURE_NO_TITLE );
			progressDialog.show();
			needToCreateMenu = true;
			this.invalidateOptionsMenu();
			int i = 0;
			String ex, ans;
			while ( i < his.length() ) {// && his.charAt(i) != Constants.HISTORY_ENTRIES_SEPARATOR){
				boolean was_dot = false;
				ex = ans = "";
				while ( i < his.length() ) {
					if ( his.charAt( i ) == Constants.HISTORY_ENTRIES_SEPARATOR ) {
						i++;
						break;
					}
					if ( his.charAt( i ) == Constants.HISTORY_IN_ENTRY_SEPARATOR ) {
						i++;
						was_dot = true;
						continue;
					}
					if ( !was_dot ) {
						ex = String.format( "%s%c", ex, his.charAt( i ) );
					} else {
						ans = String.format( "%s%c", ans, his.charAt( i ) );
					}
					i++;
				}
				String description = null;
				if ( ex.contains( Character.toString( Constants.HISTORY_DESC_SEPARATOR ) ) ) {
					int j = 0;
					while ( j < ex.length() && ex.charAt( j ) != Constants.HISTORY_DESC_SEPARATOR ) {
						j++;
					}
					description = ex.substring( j + 1 );
					ex = ex.substring( 0, j );
				}
				str.add( new HistoryEntry( ex, ans, description ) );
			}
			progressDialog.dismiss();
		}
		setupRecyclerView();
	}

	private void runReformat() {
		try {
			ProgressDialog pd = new ProgressDialog( this );
			pd.requestWindowFeature( Window.FEATURE_NO_TITLE );
			pd.setCancelable( false );
			new HistoryStorageProtocolsFormatter( this ).reformatHistory( LOCAL_HISTORY_STORAGE_PROTOCOL_VERSION, Constants.HISTORY_STORAGE_PROTOCOL_VERSION );
			pd.dismiss();
			prepareHistoryForRecyclerView();
		} catch (StringIndexOutOfBoundsException e) {
			onSomethingWentWrong();
		}
	}

	@SuppressLint({ "ClickableViewAccessibility", "SetTextI18n" })
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		sp = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_history );

		Toolbar toolbar = findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );
		getSupportActionBar().setDisplayHomeAsUpEnabled( true );

		history_action = new Intent( BuildConfig.APPLICATION_ID + ".HISTORY_ACTION" );
		history_action.putExtra( "example", "" ).putExtra( "result", "" );

		Button btn = findViewById( R.id.btnCancel );
		btn.setOnLongClickListener( forceDelete );

		rv = findViewById( R.id.rv_view );

		start_type = getIntent().getStringExtra( "start_type" );
		LOCAL_HISTORY_STORAGE_PROTOCOL_VERSION = sp.getInt( "local_history_storage_protocol_version", 1 );
		String history = sp.getString( "history", null );
		if ( history != null ) {
			if ( LOCAL_HISTORY_STORAGE_PROTOCOL_VERSION < Constants.HISTORY_STORAGE_PROTOCOL_VERSION ) {
				CustomAlertDialogBuilder builder = new CustomAlertDialogBuilder( this );
				builder
						.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
								runReformat();
							}
						} )
						.setCancelable( false )
						.setMessage( R.string.confirm_history_reformat );
				builder.create().show();
			} else if ( LOCAL_HISTORY_STORAGE_PROTOCOL_VERSION > Constants.HISTORY_STORAGE_PROTOCOL_VERSION ) {
				setContentView( R.layout.activity_history_protocols_donot_match );
				TextView note = findViewById( R.id.lblProtocolsDoNotMatch );
				String text = "";
				String[] arr = getResources().getStringArray( R.array.protocols_do_not_match );
				for (String s : arr) {
					text = String.format( "%s\n%s", text, s );
				}
				note.setText( text );
				needToCreateMenu = false;
				if ( mMenu != null ) {
					mMenu.removeItem( R.id.clear_history );
					this.invalidateOptionsMenu();
				}
			} else {
				prepareHistoryForRecyclerView();
			}
		} else {
			sp.edit().putInt( "local_history_storage_protocol_version", Constants.HISTORY_STORAGE_PROTOCOL_VERSION ).apply();
			prepareHistoryForRecyclerView();
		}
	}

	@Override
	protected void onPostCreate(@Nullable Bundle savedInstanceState) {
		super.onPostCreate( savedInstanceState );
		if ( !needToCreateMenu && mMenu != null ) {
			mMenu.removeItem( R.id.clear_history );
			this.invalidateOptionsMenu();
		}
	}
}