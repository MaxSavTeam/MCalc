package com.maxsavteam.newmcalc2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.maxsavteam.calculator.results.NumberList;
import com.maxsavteam.newmcalc2.R;
import com.maxsavteam.newmcalc2.adapters.WindowRecallAdapter;
import com.maxsavteam.newmcalc2.ui.base.ThemeActivity;
import com.maxsavteam.newmcalc2.utils.MemorySaverReader;
import com.maxsavteam.newmcalc2.utils.ResultCodesConstants;
import com.maxsavteam.newmcalc2.widget.CustomAlertDialogBuilder;

import java.util.ArrayList;

public class MemoryActionsActivity extends ThemeActivity {
	private String type;
	private MemorySaverReader memorySaverReader;

	private void clearAll() {
		new CustomAlertDialogBuilder( this )
				.setMessage( R.string.delete_all_memories_quest )
				.setPositiveButton( R.string.yes, (dialogInterface1, i)->{
					memorySaverReader.save( null );
					dialogInterface1.cancel();
					setResult( ResultCodesConstants.RESULT_REFRESH );
					onBackPressed();
				} )
				.setNegativeButton( R.string.no, (dialogInterface1, i)->dialogInterface1.cancel() )
				.show();
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		int id = item.getItemId();
		if(id == R.id.item_delete){
			clearAll();
		}
		return super.onOptionsItemSelected( item );
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate( R.menu.memory_actions_menu, menu );
		return super.onCreateOptionsMenu( menu );
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_memory_actions );

		memorySaverReader = new MemorySaverReader();

		type = getIntent().getStringExtra( "type" );

		setActionBar( R.id.toolbar );
		displayHomeAsUp();
		setTitle( type.equals( "rc" ) ? R.string.get_from_memory : R.string.put_in_memory );

		ArrayList<NumberList> memoryEntries = memorySaverReader.read();
		RecyclerView rv = findViewById( R.id.memory_actions_rv );
		WindowRecallAdapter adapter = new WindowRecallAdapter( memoryEntries, (view, position)->{
			if ( type.equals( "st" ) ) {
				setResult( RESULT_OK, new Intent().putExtra( "position", position ) );
			} else if ( type.equals( "rc" ) ) {
				Intent intent = new Intent();
				intent.putExtra( "position", position );
				setResult( ResultCodesConstants.RESULT_APPEND, intent );
			}
			onBackPressed();
		} );
		LinearLayoutManager manager = new LinearLayoutManager( this );
		manager.setOrientation( RecyclerView.VERTICAL );
		rv.setLayoutManager( manager );
		rv.setAdapter( adapter );
	}
}
