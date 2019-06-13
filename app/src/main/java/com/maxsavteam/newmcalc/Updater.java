package com.maxsavteam.newmcalc;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Updater extends AppCompatActivity {

	public StorageReference mStorageRef;
	public Drawable btndr, btnbl;
	public Boolean downloading = false;
	public FirebaseDatabase db = FirebaseDatabase.getInstance();
	public DatabaseReference ref = db.getReference("versionCode");
	public DatabaseReference refCount = db.getReference("version");
	public File localFile;
	public StorageReference riversRef;
	public String file_url_path = "http://maxsavteam.tk/apk/NewMCalc.apk";
	public SharedPreferences sp;
	public AlertDialog deval;
	public String newversion;
	public File outputFile = null;
	public String vk = "maksin.colf", insta = "maksin.colf/", facebook = "profile.php?id=100022307565005", tw = "maks_savitsky", site = "maxsavteam.tk";
	public int layUpVis;

	ValueEventListener list = new ValueEventListener() {
		@Override
		public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
			switch (Objects.requireNonNull(dataSnapshot.getKey())) {
				case "vk":
					vk = dataSnapshot.getValue(String.class);
					break;
				case "insta":
					insta = dataSnapshot.getValue(String.class);
					break;
				case "twitter":
					tw = dataSnapshot.getValue(String.class);
					break;
				case "facebook":
					facebook = dataSnapshot.getValue(String.class);
					break;
				case "site":
					site = dataSnapshot.getValue(String.class);
					break;
			}

		}

		@Override
		public void onCancelled(@NonNull DatabaseError databaseError) {
			Toast.makeText(getApplicationContext(), "cancelled", Toast.LENGTH_SHORT).show();
		}
	};
	AlertDialog dl;
	View.OnClickListener notJoin = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			//findViewById(R.id.layDev).setVisibility(View.GONE);
			sp.edit().putBoolean("show_laydev", false).apply();
			deval.cancel();
		}
	};
	View.OnClickListener social = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent in = new Intent(Intent.ACTION_VIEW);
			if (v.getId() == R.id.imgBtnVk) {
				in.setData(Uri.parse("https://vk.com/" + vk));
			} else if (v.getId() == R.id.imgBtnInsta) {
				in.setData(Uri.parse("https://instagram.com/" + insta));
			} else if (v.getId() == R.id.imgBtnTw) {
				in.setData(Uri.parse("https://twitter.com/" + tw));
			} else if (v.getId() == R.id.imgBtnWeb) {
				in.setData(Uri.parse("https://" + site));
			} else if (v.getId() == R.id.btnImgMore) {
				in.setData(Uri.parse("https://" + site + "/Apps.m/"));
			}
			startActivity(in);
		}
	};
	View.OnLongClickListener show_join = new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			sp.edit().remove("stop_receive_all").apply();
			sp.edit().remove("show_laydev").apply();
			Toast.makeText(getApplicationContext(), "You can join to testers community", Toast.LENGTH_SHORT).show();
			return true;
		}
	};
	View.OnClickListener join = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
            /*setContentView(R.layout.layout_updater);
            TextView t = findViewById(R.id.txtDownloading);
            t.setText(R.string.please_wait);*/
			sp.edit().putBoolean("isdev", true).apply();
            /*try{
                Thread.sleep(1500);
            }catch(Exception e){
                e.printStackTrace();
            }*/
			setContentView(R.layout.updater_main);
			//findViewById(R.id.layDev).setVisibility(View.GONE);
			sp.edit().putBoolean("show_laydev", false).apply();
			findViewById(R.id.btnStopReceive).setVisibility(View.VISIBLE);
			deval.cancel();
			Toast.makeText(getApplicationContext(), "Now you are a tester!\nTo apply the settings, restart the application.", Toast.LENGTH_SHORT).show();
		}
	};
	AlertDialog report_al;
	Intent send = new Intent(Intent.ACTION_SEND);
	Intent action;
	View mv;
	String up_path = "", up_ver = "";
	String up_type = "simple", newDevVer = "";
	int newCodeDev = 0;
	update_service ups;
	View.OnLongClickListener defLang = new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			sp.edit().remove("lang").apply();
			return true;
		}
	};
	boolean isotherset = false;
	ProgressDialog pd;

	protected void backPressed() {
		if (isotherset) {
			isotherset = false;
			setContentView(R.layout.updater_main);
			LinearLayout ll = findViewById(R.id.layoutUpdate);
			ll.setVisibility(layUpVis);
			postcreate();
		} else {
			finish();
			overridePendingTransition(R.anim.abc_popup_enter, R.anim.alpha);
		}
	}

	@Override
	public void onBackPressed() {
		backPressed();
		//super.onBackPressed();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		//Toast.makeText(getApplicationContext(), Integer.toString(id) + " " + Integer.toString(R.id.home), Toast.LENGTH_SHORT).show();
		if (id == android.R.id.home) {
			backPressed();
		}
		return super.onOptionsItemSelected(item);
	}

	public void clear_history(View v) {
		dl.show();
	}

	public void report(View v) {
		send.putExtra(Intent.EXTRA_EMAIL, new String[]{"maxsavsu@gmail.com"});
		send.putExtra(Intent.EXTRA_SUBJECT, "Problem in New MCalc");
		send.putExtra(Intent.EXTRA_TEXT, "[" + BuildConfig.VERSION_NAME + "," + BuildConfig.VERSION_CODE + "]\nProblem:\n");
		send.setType("message/rfc822");
		report_al.show();
		//Toast.makeText(getApplicationContext(), R.string.donot_clear_info_email, Toast.LENGTH_LONG).show();
	}

	public void stop_receive(View v) {
		AlertDialog.Builder build = new AlertDialog.Builder(Updater.this);
		build.setTitle(R.string.confirm)
				.setMessage(R.string.stop_receive_mes)
				.setCancelable(false)
				.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				})
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//findViewById(R.id.layDev).setVisibility(View.GONE);
						sp.edit().putBoolean("show_laydev", false).apply();
						sp.edit().putBoolean("isdev", false).apply();
						sp.edit().putBoolean("stop_receive_all", true).apply();
						findViewById(R.id.btnStopReceive).setVisibility(View.INVISIBLE);
						findViewById(R.id.layoutUpdate).setVisibility(View.INVISIBLE);
					}
				});
		AlertDialog dialog = build.create();
		dialog.show();
	}

	public void switchSave(View v) {
		Switch sw = findViewById(R.id.switchSaveOnExit);
		sp.edit().putBoolean("saveResult", sw.isChecked()).apply();
		if (sw.isChecked()) {
			sw.setText(R.string.switchSaveOn);
		} else {
			sw.setText(R.string.switchSaveOff);
			sp.edit().remove("saveResult").apply();
		}
	}

	public void sh_about_dev(View v) {
		AlertDialog.Builder build = new AlertDialog.Builder(this);
		build.setCancelable(true).setTitle(R.string.about_dev);
		build.setView(mv);
		AlertDialog d = build.create();
		LinearLayout ll = mv.findViewById(R.id.linearLayout8);
		d.show();
	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.updater_main);
        /*Slide slide = new Slide();
        slide.setDuration(100);
        getWindow().setEnterTransition(slide);
        getWindow().setExitTransition(slide);*/
		getSupportActionBar().setTitle(getResources().getString(R.string.settings));
		sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		//findViewById(R.id.imgBtnWeb).setOnLongClickListener(show_join);
		mv = getLayoutInflater().inflate(R.layout.about_developer, null);
		mv.findViewById(R.id.imgBtnWeb).setOnLongClickListener(show_join);
		mv.findViewById(R.id.imgBtnInsta).setOnClickListener(social);
		mv.findViewById(R.id.imgBtnTw).setOnClickListener(social);
		mv.findViewById(R.id.imgBtnVk).setOnClickListener(social);
		mv.findViewById(R.id.btnImgMore).setOnClickListener(social);
		ups = new update_service(this);
        /*try {

        }catch (Exception e){
            e.printStackTrace();
        }*/
		//set_lang("create");
		findViewById(R.id.btnChLang).setOnLongClickListener(defLang);
		BroadcastReceiver br = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				AlertDialog.Builder b = new AlertDialog.Builder(Updater.this);
				b.setCancelable(false)
						.setTitle(R.string.installation)
						.setMessage(R.string.update_avail_to_install)
						.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
							}
						}).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						ups.install();
					}
				});
				AlertDialog inst = b.create();
				inst.show();
			}
		};
		BroadcastReceiver brfail = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				AlertDialog.Builder b = new AlertDialog.Builder(Updater.this);
				b.setTitle(R.string.installation).setMessage(R.string.cannot_update).setNegativeButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
				AlertDialog inst = b.create();
				inst.show();
			}
		};
		registerReceiver(br, new IntentFilter(BuildConfig.APPLICATION_ID + ".NEWMCALC_UPDATE_SUC"));
		registerReceiver(brfail, new IntentFilter(BuildConfig.APPLICATION_ID + ". NEWMCALC_UPDATE_FAIL"));
		try {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.black)));
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();

		}
		DatabaseReference devCode = db.getReference("dev/versionCodeDev");
		DatabaseReference devVer = db.getReference("dev/versionDev");
		postcreate();

		if (sp.getBoolean("isdev", false)) {
			devVer.addValueEventListener(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
					newDevVer = dataSnapshot.getValue(String.class);
				}

				@Override
				public void onCancelled(@NonNull DatabaseError databaseError) {

				}
			});
		}

		refCount.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
				newversion = (String) dataSnapshot.getValue();
			}

			@Override
			public void onCancelled(@NonNull DatabaseError databaseError) {
				newversion = "";
			}
		});

		AlertDialog.Builder build = new AlertDialog.Builder(this);
		build.setTitle(R.string.confirm)
				.setMessage(R.string.confirm_cls_history)
				.setCancelable(false)
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						sp.edit().putString("history", "").apply();
						findViewById(R.id.btnClsHistory).setVisibility(View.INVISIBLE);
					}
				}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		dl = build.create();
		build = new AlertDialog.Builder(this);
		build.setCancelable(true).setMessage(R.string.donot_clear_info_email).setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				startActivity(Intent.createChooser(send, "Choose email client"));
			}
		});
		report_al = build.create();
		action = getIntent();
		up_path = action.getStringExtra("update_path");
		up_ver = action.getStringExtra("upVerName");
		if (action.getStringExtra("action").equals("update")) {
			setContentView(R.layout.layout_updater);
			downloading = true;
			try {
				Thread.sleep(250);
			} catch (Exception e) {
				e.printStackTrace();
			}
			//return;
		} else {
			DatabaseReference dbm = db.getReference("links/vk");
			dbm.addValueEventListener(list);
			dbm = db.getReference("links/insta");
			dbm.addValueEventListener(list);
			dbm = db.getReference("links/facebook");
			dbm.addValueEventListener(list);
			dbm = db.getReference("links/twitter");
			dbm.addValueEventListener(list);
			dbm = db.getReference("links/site");
			dbm.addValueEventListener(list);
			if (sp.getBoolean("isdev", false)) {
				devCode.addValueEventListener(new ValueEventListener() {
					@Override
					public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
						newCodeDev = dataSnapshot.getValue(Integer.TYPE);
					}

					@Override
					public void onCancelled(@NonNull DatabaseError databaseError) {

					}
				});
			}
			if (sp.getBoolean("isdev", false)) {
				try {
					Thread.sleep(900);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			String vercode = Integer.toString(BuildConfig.VERSION_CODE);
			ref.addValueEventListener(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
					String value = dataSnapshot.getValue().toString();
					Integer versionMy = Integer.valueOf(vercode);
					Integer versionNew = Integer.valueOf(value);
					TextView up = findViewById(R.id.txtUpdate);
					if (!ups.isup()) {
						if (versionNew > versionMy && (newCodeDev == versionNew || newCodeDev == 0)) {
							up_type = "simple";
							up_path = "/NewMCalc.apk";
							up_ver = newversion;
							LinearLayout l = findViewById(R.id.layoutUpdate);
							l.setVisibility(LinearLayout.VISIBLE);
							up.setText(R.string.updateavail);
						} else if (versionNew < newCodeDev && versionMy < newCodeDev) {
							up_type = "dev";
							up_path = "/forTesters/NewMCalc.apk";
							up_ver = newDevVer;
							LinearLayout l = findViewById(R.id.layoutUpdate);
							l.setVisibility(LinearLayout.VISIBLE);
							up.setText(R.string.updateavail_tc);
						}
					}
				}

				@Override
				public void onCancelled(@NonNull DatabaseError databaseError) {
					Log.e("FirebaseDB", "Cancelled: " + databaseError.toString());
				}
			});
		}
	}

	public void postcreate() {
		if (sp.getBoolean("isdev", false)) {
			if (!sp.getBoolean("stop_receive_all", false) && !sp.getBoolean("show_laydev", true))
				findViewById(R.id.btnStopReceive).setVisibility(View.VISIBLE);
			//findViewById(R.id.layDev).setVisibility(View.GONE);
		} else {
			if (sp.getBoolean("show_laydev", true)) {
				View mView = getLayoutInflater().inflate(R.layout.join_testers, null);
				mView.findViewById(R.id.btnNotJoin).setOnClickListener(notJoin);
				mView.findViewById(R.id.btnGetBuilds).setOnClickListener(join);
				AlertDialog.Builder builddev = new AlertDialog.Builder(this);
				builddev.setView(mView).setCancelable(false);
				deval = builddev.create();
				deval.show();
			}
		}
		if (!sp.getString("history", "none").equals("none")) {
			findViewById(R.id.btnClsHistory).setVisibility(View.VISIBLE);
		}else{
			findViewById(R.id.btnClsHistory).setVisibility(View.GONE);
		}
		Switch sw = findViewById(R.id.switchSaveOnExit);
		sw.setChecked(sp.getBoolean("saveResult", false));
		if (sw.isChecked()) {
			sw.setText(R.string.switchSaveOn);
		} else {
			sw.setText(R.string.switchSaveOff);
		}
	}

	public void choose_btn(View v) {
		Button btn = findViewById(v.getId());
		Button btn2;
		if (v.getId() == R.id.btnRight) {
			btn2 = findViewById(R.id.btnLeft);
		} else {
			btn2 = findViewById(R.id.btnRight);
		}
        /*ColorDrawable cd = new ColorDrawable();
        try{
            cd = (ColorDrawable) btn.getBackground();
        }catch (Exception e){
            e.getCause().printStackTrace();
        }*/
		if (btn.getBackground() != btnbl) {
			btn2.setBackground(btn.getBackground());
			btn2.setTextColor(getResources().getColor(R.color.black));
			btn.setBackgroundColor(getResources().getColor(R.color.black));
			btn.setTextColor(getResources().getColor(R.color.white));
			if (btn.getId() == R.id.btnRight) {
				sp.edit().putInt("btn_add_align", 1).apply();
			} else
				sp.edit().putInt("btn_add_align", 0).apply();
			Intent btnal = new Intent(BuildConfig.APPLICATION_ID + ".ON_BTN_ALIGN_CHANGE");
			sendBroadcast(btnal);
		}
	}

	public void update(View v) {
		findViewById(R.id.layoutUpdate).setVisibility(View.INVISIBLE);
		ups.run(up_path, up_ver);
	}

	public void other_settings(View v) {
		isotherset = true;
		LinearLayout ll = findViewById(R.id.layoutUpdate);
		layUpVis = ll.getVisibility();
		setContentView(R.layout.other_settings);
		int loc = sp.getInt("btn_add_align", 0);
		if (loc == 0) {
			Button l = findViewById(R.id.btnLeft);
			btndr = l.getBackground();
			l.setBackgroundColor(getResources().getColor(R.color.black));
			l.setTextColor(getResources().getColor(R.color.white));
			btnbl = l.getBackground();
		} else if (loc == 1) {
			Button l = findViewById(R.id.btnRight);
			btndr = l.getBackground();
			l.setBackgroundColor(getResources().getColor(R.color.black));
			l.setTextColor(getResources().getColor(R.color.white));
			btnbl = l.getBackground();
		}
		findViewById(R.id.btnExport).setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				sp.edit().clear().apply();
				sendBroadcast(new Intent(BuildConfig.APPLICATION_ID + ".SP_EDITED"));
				Toast.makeText(getApplicationContext(), "Storage cleared", Toast.LENGTH_SHORT).show();
				finish();
				return true;
			}
		});
	}

	public void import_(View v) {
		File f = new File(Environment.getExternalStorageDirectory() + "/MST files/NewMCalc.imc");
		if (!f.exists()) {
			Toast.makeText(getApplicationContext(), R.string.export_file_not_found, Toast.LENGTH_LONG).show();
			return;
		}
		try {
			FileReader fr = new FileReader(f);
			//tring ans = "";
			sp.edit().clear().apply();
			while (fr.ready()) {
				char t = (char) fr.read();
				String tag = "";
				char read = (char) fr.read();
				while (read != '=') {
					tag += read;
					read = (char) fr.read();
				}
				String value = "";
				read = (char) fr.read();
				while (read != '©') {
					value += read;
					read = (char) fr.read();
				}
				//ans += t + " " + tag + " = " + value  + "\n";
				if (t == 'S') {
					sp.edit().putString(tag, value).apply();
				} else if (t == 'I') {
					sp.edit().putInt(tag, Integer.valueOf(value)).apply();
				} else if (t == 'B') {
					sp.edit().putBoolean(tag, Boolean.parseBoolean(value)).apply();
				}
			}
			Toast.makeText(getApplicationContext(), R.string.on_import, Toast.LENGTH_LONG).show();
			fr.close();
			//postcreate();
			sendBroadcast(new Intent(BuildConfig.APPLICATION_ID + ".SP_EDITED"));
			finish();
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
		}
	}

	public void create_import(View v) {
		ProgressDialog pd;
		pd = new ProgressDialog(this);
		pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pd.setCancelable(false);
		File f = new File(Environment.getExternalStorageDirectory() + "/MST files");
		if (!f.isDirectory()) {
			f.mkdir();
		}
		f = new File(Environment.getExternalStorageDirectory() + "/MST files/NewMCalc.imc");
		pd.show();
		try {
			FileWriter fw = new FileWriter(f, false);
			String s = sp.getAll().toString();
			Map<String, ?> m = sp.getAll();
			fw.write("");
			//fw.append(s);
			Set<String> se = m.keySet();
			List<String> l = new ArrayList<String>(se);
			for (int i = 0; i < l.size(); i++) {
				String ty = m.get(l.get(i)).getClass().getName();
				ty = ty.replace("java.lang.", "");
				ty = Character.toString(ty.charAt(0));
				fw.append(ty).append(l.get(i)).append("=").append(String.valueOf(m.get(l.get(i)))).append("©");
			}
			pd.dismiss();
			fw.flush();
			Toast.makeText(getApplicationContext(), R.string.exported, Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			pd.dismiss();
			pd.setMessage(e.toString());
			pd.setCancelable(true);
			pd.show();
			try {
				Thread.sleep(2000);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			e.printStackTrace();
		}
	}
}
