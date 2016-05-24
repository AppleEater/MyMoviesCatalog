package com.example.uaharoni.mymoviescatalog.App;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.example.uaharoni.mymoviescatalog.R;

public abstract class MenuActivity extends AppCompatActivity {

    public static final int MENU_CONTEXT_EDIT = 0;
    public static final int MENU_CONTEXT_DELETE = 1;
    public static final int MENU_CONTEXT_SHARE = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_exit:
                setResult(MainActivity.APP_EXIT_RETURNCODE);
                finish();
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        //menu.setHeaderTitle("");
        menu.clearHeader();
        menu.add(0, MENU_CONTEXT_EDIT, 1, R.string.menu_context_option_edit_movie);
        menu.add(0, MENU_CONTEXT_DELETE, 2, R.string.menu_context_option_delete_movie);
        menu.add(0, MENU_CONTEXT_SHARE, 3, R.string.menu_context_option_item_share);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return super.onContextItemSelected(item);
    }
}
